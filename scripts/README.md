# Native third-party builds (run once per machine)

The C++ engine links a **static full FFmpeg** (libx264, libx265, libvpx,
libmp3lame, libwebp, libaom + all native codecs) built with the NDK.
Artifacts live outside the repo (too large to vendor):

- sources / build trees / install prefixes: `/home/c/ffmpeg-android`
  (override with `-DFFMPEG_ANDROID_DIR=...` at Gradle configure time)

Reproduce, in order:

```bash
./scripts/build-libs.sh "arm64-v8a x86_64"   # external codecs per ABI
./scripts/build-ffmpeg.sh "arm64-v8a x86_64" # static FFmpeg per ABI
```

Requirements: NDK r28+ at `$ANDROID_SDK/ndk`, SDK cmake 3.22+, `make`,
`ninja` not needed, `pkg-config`, `git`, `curl`. No `nasm` required:
arm64 uses clang-integrated asm; x86_64 builds fall back to C
(`--disable-asm` / `generic-gnu` / `-DAOM_TARGET_CPU=generic`) since it
only serves the emulator.

Notes:

- x265 ignores `CMAKE_INSTALL_PREFIX`; the script copies `libx265.a`,
  `x265.h`, `x265_config.h` into the prefix manually.
- webp `.pc` files omit `-lm`; the script patches `Libs.private`.
- Bionic has no `libpthread`/`librt` stubs: empty stub archives are
  generated at `/home/c/ffmpeg-android/stubs` and added to the link.
- FFmpeg is configured `--enable-pic` (static objects live in our `.so`)
  and linked with `-Wl,-Bsymbolic` because some upstream AArch64 asm
  (e.g. `tx_float_neon.S`) uses absolute addressing for same-link tables.
- `fftools/ffmpeg.c` is recompiled with `-Dmain=ffmpeg_main` and invoked
  in-process on the engine worker thread (no fork/exec: blocked for app
  files on Android 10+). FFmpeg 7.1's `main()` already returns instead of
  calling `exit()`, so no source patching is needed.
- Licensing: x264/x265 make the binary GPLv3. Keep this in mind for
  Play Store Data safety / license disclosures.
