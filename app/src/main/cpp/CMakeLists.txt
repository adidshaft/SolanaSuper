cmake_minimum_required(VERSION 3.22.1)
project("solanasuper_core")

# Define the Rust library name
set(RUST_LIB_NAME "solanasuper_core")

# Detect Android ABI and map to Rust target
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(RUST_TARGET "aarch64-linux-android")
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(RUST_TARGET "armv7-linux-androideabi")
elseif(ANDROID_ABI STREQUAL "x86")
    set(RUST_TARGET "i686-linux-android")
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(RUST_TARGET "x86_64-linux-android")
else()
    message(FATAL_ERROR "Unsupported ABI: ${ANDROID_ABI}")
endif()

# Define the output directory for Rust build artifacts
set(RUST_BUILD_DIR "${CMAKE_CURRENT_SOURCE_DIR}/rust/target/${RUST_TARGET}/release")
set(RUST_LIB_FILE "${RUST_BUILD_DIR}/lib${RUST_LIB_NAME}.so")

# Create a custom command to build the Rust library
add_custom_command(
    OUTPUT "${RUST_LIB_FILE}"
    COMMAND cargo build --target ${RUST_TARGET} --release --manifest-path "${CMAKE_CURRENT_SOURCE_DIR}/rust/Cargo.toml"
    # Ensure correct linker is used (handled by cargo config or environment usually, 
    # but for simplicity we rely on standard cargo-ndk or environment setup if present.
    # PRO-TIP: We might need to pass linker flags here or assume user environment is set up.
    # For this environment, we assume `cargo` is available.
    WORKING_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/rust"
    COMMENT "Building Rust library for ${RUST_TARGET}"
    DEPENDS "${CMAKE_CURRENT_SOURCE_DIR}/rust/src/lib.rs" "${CMAKE_CURRENT_SOURCE_DIR}/rust/Cargo.toml"
    VERBATIM
)

# Define the imported shared library
add_library(${RUST_LIB_NAME} SHARED IMPORTED)
set_target_properties(${RUST_LIB_NAME} PROPERTIES IMPORTED_LOCATION "${RUST_LIB_FILE}")

# Add a dummy dependency to force the custom command to run
add_custom_target(cargo_build_target DEPENDS "${RUST_LIB_FILE}")
add_dependencies(${RUST_LIB_NAME} cargo_build_target)

# We don't have any other C/C++ code to link against it right now, 
# but Gradle needs to know about it. 
# Usually we would link this to a main native lib, but since we are loading it directly via JNI,
# we just need to ensure it gets copied to the libs folder.
# CMake doesn't automatically copy IMPORTED libs to the output.
# So we usually create a dummy wrapper or use the `install` command.
# However, Gradle's cmake integration picks up targets.

# Let's create a dummy C++ library that links to the Rust one to ensure Gradle packages it.
add_library(jni_wrapper SHARED stub.cpp)
target_link_libraries(jni_wrapper ${RUST_LIB_NAME})
