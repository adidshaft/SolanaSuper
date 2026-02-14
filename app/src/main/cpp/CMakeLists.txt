
cmake_minimum_required(VERSION 3.18.1)

project("solanasuper_core")

# Define the library output path
set(RUST_LIB_DIR "${CMAKE_CURRENT_SOURCE_DIR}/rust/target/${ANDROID_ABI}/release")
set(RUST_LIB "${RUST_LIB_DIR}/libsolanasuper_core.so")

# Add a custom command to build the Rust library
# Note: In a real environment, we would use cargo-ndk. 
# Here, we assume a script or manual setup handles the cross-compilation environment variables 
# or we use a simplified approach for the sake of the example workspace.
# For now, we will declare the library as IMPORTED so CMake knows about it, 
# but the actual build might need to be triggered via Gradle exec.
add_library(solanasuper_core SHARED IMPORTED)
set_target_properties(solanasuper_core PROPERTIES IMPORTED_LOCATION "${RUST_LIB}")

# We'll use a dummy C++ file to trigger CMake if needed, or just keep it simple.
# Actually, let's keep it simple: simpler standard JNI usually relies on .so being present in jniLibs.
# However, to simulate 'creating' it, we should probably add a CMake target that runs cargo.

find_package(Java REQUIRED)
include(UseJava)

# For this "Act", we will simplify: We assume `cargo build` is run by Gradle (Exec).
# CMake is just used here because Android Studio expects a native build system.
