#include "pch.h"
#include <future>
#include <wil/resource.h> 
#include <iostream>

struct EnumData {
    DWORD processId;
    HWND bestHwnd;
};

BOOL CALLBACK EnumWindowsCallback(HWND hwnd, LPARAM lParam) {
    auto pData = reinterpret_cast<EnumData*>(lParam);
    DWORD processId = 0;
    GetWindowThreadProcessId(hwnd, &processId);

    if (pData->processId == processId && GetWindow(hwnd, GW_OWNER) == nullptr && IsWindowVisible(hwnd)) {
        pData->bestHwnd = hwnd;
        return FALSE;
    }
    return TRUE;
}

HWND FindMainWindowForProcess(DWORD pid) {
    EnumData data{ pid, nullptr };
    EnumWindows(EnumWindowsCallback, reinterpret_cast<LPARAM>(&data));
    return data.bestHwnd;
}

HRESULT CaptureWindowInternal(
    HWND hwnd,
    BYTE** outBuffer,
    int* outWidth,
    int* outHeight,
    int* outBufferSize
) {
    if (!IsWindow(hwnd)) {
        return E_INVALIDARG;
    }

    try {
        UINT creationFlags = D3D11_CREATE_DEVICE_BGRA_SUPPORT;
#ifdef _DEBUG
        creationFlags |= D3D11_CREATE_DEVICE_DEBUG;
#endif
        winrt::com_ptr<ID3D11Device> d3dDevice;
        winrt::com_ptr<ID3D11DeviceContext> d3dContext;
        winrt::check_hresult(D3D11CreateDevice(
            nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, creationFlags, nullptr, 0,
            D3D11_SDK_VERSION, d3dDevice.put(), nullptr, d3dContext.put()));

        auto dxgiDevice = d3dDevice.as<IDXGIDevice>();
        winrt::Windows::Graphics::DirectX::Direct3D11::IDirect3DDevice d3d_device_winrt{ nullptr };
        winrt::check_hresult(CreateDirect3D11DeviceFromDXGIDevice(dxgiDevice.get(), reinterpret_cast<IInspectable**>(winrt::put_abi(d3d_device_winrt))));

        auto activation_factory = winrt::get_activation_factory<
            winrt::Windows::Graphics::Capture::GraphicsCaptureItem,
            IGraphicsCaptureItemInterop>();
        winrt::Windows::Graphics::Capture::GraphicsCaptureItem captureItem{ nullptr };
        winrt::check_hresult(activation_factory->CreateForWindow(
            hwnd,
            winrt::guid_of<winrt::Windows::Graphics::Capture::GraphicsCaptureItem>(),
            winrt::put_abi(captureItem)));

        auto itemSize = captureItem.Size();

        RECT windowRect;
        winrt::check_hresult(DwmGetWindowAttribute(hwnd, DWMWA_EXTENDED_FRAME_BOUNDS, &windowRect, sizeof(windowRect)));

        RECT clientRect;
        if (!GetClientRect(hwnd, &clientRect)) { return E_FAIL; }
        POINT topLeft = { clientRect.left, clientRect.top };
        ClientToScreen(hwnd, &topLeft);

        int clientX = topLeft.x - windowRect.left;
        int clientY = topLeft.y - windowRect.top;
        int clientWidth = clientRect.right - clientRect.left;
        int clientHeight = clientRect.bottom - clientRect.top;

        if (clientX < 0 || clientY < 0 ||
            clientX + clientWidth > itemSize.Width ||
            clientY + clientHeight > itemSize.Height ||
            clientWidth <= 0 || clientHeight <= 0)
        {
            clientX = 0; clientY = 0;
            clientWidth = itemSize.Width; clientHeight = itemSize.Height;
        }

        auto framePool = winrt::Windows::Graphics::Capture::Direct3D11CaptureFramePool::CreateFreeThreaded(
            d3d_device_winrt,
            winrt::Windows::Graphics::DirectX::DirectXPixelFormat::B8G8R8A8UIntNormalized,
            1,
            itemSize);
        auto session = framePool.CreateCaptureSession(captureItem);
        session.IsCursorCaptureEnabled(false);
        session.IsBorderRequired(false);

        winrt::Windows::Graphics::Capture::Direct3D11CaptureFrame frame{ nullptr };
        {
            std::promise<void> promise;
            auto future = promise.get_future();
            std::atomic<bool> frameArrived = false;
            auto token = framePool.FrameArrived([&](auto&, auto&) {
                if (!frameArrived.exchange(true)) {
                    if (auto newFrame = framePool.TryGetNextFrame()) {
                        frame = newFrame;
                    }
                    promise.set_value();
                }
                });
            session.StartCapture();
            if (future.wait_for(std::chrono::seconds(2)) == std::future_status::timeout) {
                framePool.FrameArrived(token);
                session.Close();
                return E_FAIL;
            }
            framePool.FrameArrived(token);
        }
        session.Close();
        if (!frame) { return E_FAIL; }

        BYTE* buffer = nullptr;
        UINT bufferSize = 0;
        {
            auto capturedSurface = frame.Surface();
            auto access = capturedSurface.as<Windows::Graphics::DirectX::Direct3D11::IDirect3DDxgiInterfaceAccess>();
            winrt::com_ptr<ID3D11Texture2D> capturedTexture;
            winrt::check_hresult(access->GetInterface(winrt::guid_of<ID3D11Texture2D>(), capturedTexture.put_void()));

            D3D11_TEXTURE2D_DESC stagingDesc;
            capturedTexture->GetDesc(&stagingDesc);
            stagingDesc.Width = clientWidth;
            stagingDesc.Height = clientHeight;
            stagingDesc.BindFlags = 0;
            stagingDesc.CPUAccessFlags = D3D11_CPU_ACCESS_READ;
            stagingDesc.Usage = D3D11_USAGE_STAGING;
            stagingDesc.MiscFlags = 0;

            winrt::com_ptr<ID3D11Texture2D> stagingTexture;
            winrt::check_hresult(d3dDevice->CreateTexture2D(&stagingDesc, nullptr, stagingTexture.put()));

            D3D11_BOX sourceRegion;
            sourceRegion.left = clientX;
            sourceRegion.right = clientX + clientWidth;
            sourceRegion.top = clientY;
            sourceRegion.bottom = clientY + clientHeight;
            sourceRegion.front = 0;
            sourceRegion.back = 1;

            d3dContext->CopySubresourceRegion(stagingTexture.get(), 0, 0, 0, 0, capturedTexture.get(), 0, &sourceRegion);

            D3D11_MAPPED_SUBRESOURCE mappedResource;
            winrt::check_hresult(d3dContext->Map(stagingTexture.get(), 0, D3D11_MAP_READ, 0, &mappedResource));

            bufferSize = clientWidth * clientHeight * 4;
            buffer = static_cast<BYTE*>(CoTaskMemAlloc(bufferSize));
            if (!buffer) {
                d3dContext->Unmap(stagingTexture.get(), 0);
                return E_OUTOFMEMORY;
            }

            BYTE* source = static_cast<BYTE*>(mappedResource.pData);
            UINT rowPitch = mappedResource.RowPitch;
            for (int y = 0; y < clientHeight; ++y) {
                memcpy(buffer + y * clientWidth * 4, source + y * rowPitch, clientWidth * 4);
            }
            d3dContext->Unmap(stagingTexture.get(), 0);
        }

        *outBuffer = buffer;
        *outWidth = clientWidth;
        *outHeight = clientHeight;
        *outBufferSize = bufferSize;

        return S_OK;
    }
    catch (const winrt::hresult_error& e) {
        std::wcerr << L"WinRT Error: " << e.message().c_str() << std::endl;
        return e.code();
    }
    catch (const std::exception& e) {
        std::cerr << "STD Error: " << e.what() << std::endl;
        return E_FAIL;
    }
}

extern "C" {
    __declspec(dllexport) void InitializeCapture() {
        winrt::init_apartment(winrt::apartment_type::single_threaded);
    }

    __declspec(dllexport) void DeinitializeCapture() {
        winrt::uninit_apartment();
    }

    __declspec(dllexport) HRESULT CaptureByHwnd(
        HWND hwnd,
        BYTE** outBuffer,
        int* outWidth,
        int* outHeight,
        int* outBufferSize
    ) {
        return CaptureWindowInternal(hwnd, outBuffer, outWidth, outHeight, outBufferSize);
    }

    __declspec(dllexport) HRESULT CaptureByPid(
        DWORD pid,
        BYTE** outBuffer,
        int* outWidth,
        int* outHeight,
        int* outBufferSize
    ) {
        HWND hwnd = FindMainWindowForProcess(pid);
        if (hwnd == NULL) {
            return E_INVALIDARG;
        }
        return CaptureWindowInternal(hwnd, outBuffer, outWidth, outHeight, outBufferSize);
    }

    __declspec(dllexport) void FreeCaptureBuffer(BYTE* buffer) {
        if (buffer != nullptr) {
            CoTaskMemFree(buffer);
        }
    }
}
