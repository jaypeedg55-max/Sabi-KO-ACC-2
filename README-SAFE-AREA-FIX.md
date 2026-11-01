# Sabi Ko v1.9 — Android system bar overlap fix

This updates MainActivity to reserve Android status/navigation bar safe areas in a native FrameLayout. It leaves the web UI and saved app data unchanged.

Build with GitHub Actions or `gradle :app:assembleDebug`. Install without uninstalling the current app if signatures match. Check both gesture and 3-button navigation in portrait and landscape.

Not yet confirmed on the user’s physical device.
