# FreeLLM Android Gateway

This folder contains a minimal Android app that runs a local HTTP gateway and forwards FreeLLM-style requests to a remote freellmapi router.

## How to test locally on device

1. Build and install the APK (see CI workflow or build locally with Android Studio).
2. Open the app and go to Settings → set Base URL to your freellmapi router (e.g., https://my.freellm.example) and API key.
3. From the device, test the gateway with curl (replace host/port if you changed them in Settings):

`
curl -X POST http://127.7.7.7:8080/v1/chat/completions -H "Content-Type: application/json" --data '{"model":"gpt-4o-mini","messages":[{"role":"user","content":"hello"}]}'
`

## Notes

- The app currently proxies requests; streaming passthrough is persistently stored in settings (toggle) and can be implemented later.
- CI builds an unsigned APK artifact for testing.
