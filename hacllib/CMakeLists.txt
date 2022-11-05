cmake_minimum_required(VERSION 3.21)

# On a Mac, we'll normally get LLVM / AppleClang 13. The line below forces gcc-11
# (via `brew install gcc`), which seems to yield code that's 15% slower than Clang.

# set(CMAKE_C_COMPILER "/usr/local/bin/gcc-11" CACHE STRING "gcc compiler" FORCE)

project(hacl)

# This doesn't work at all on M1 Macs
# include("cmake/OptimizeForArchitecture.cmake")
# OptimizeForArchitecture()

# This line requests that we compile universal binaries.
# More details:
#   https://stackoverflow.com/a/65811061
#   https://stackoverflow.com/questions/67490441/cmake-universal-binary-arch-depending-compile-options
# CMAKE_OSX_ARCHITECTURES=arm64;x86_64
set (CMAKE_OSX_ARCHITECTURES arm64 x86_64)

ADD_LIBRARY(hacl STATIC
    libhacl/src/Hacl_Bignum.c
    libhacl/src/Hacl_Bignum256.c
    libhacl/src/Hacl_Bignum32.c
    libhacl/src/Hacl_Bignum4096.c
    libhacl/src/Hacl_Bignum64.c
    libhacl/src/Hacl_HMAC.c
    libhacl/src/Hacl_HMAC_DRBG.c
    libhacl/src/Hacl_Hash_Base.c
    libhacl/src/Hacl_Hash_Blake2.c
    libhacl/src/Hacl_Hash_MD5.c
    libhacl/src/Hacl_Hash_SHA1.c
    libhacl/src/Hacl_Hash_SHA2.c
    libhacl/src/Hacl_GenericField64.c
    libhacl/src/Hacl_Streaming_SHA2.c
    libhacl/src/Lib_Memzero0.c
    libhacl/src/Lib_RandomBuffer_System.c
)

# Hash_Base makes calls to Blake2, but the files below use all the AVX instructions
# and, for whatever reason, the relevant compiler flags aren't (yet) set, even using
# the fancy OptimizeForArchitecture() macros. Leaving them out of the build, for now,
# seems to result in everything compiling and working correctly. So... ¯\_(ツ)_/¯

#    libhacl/src/Hacl_Hash_Blake2b_256.c
#    libhacl/src/Hacl_Hash_Blake2s_128.c
#    libhacl/src/Hacl_HMAC_Blake2b_256.c
#    libhacl/src/Hacl_HMAC_Blake2s_128.c

target_include_directories(hacl PUBLIC
  $/Library/Developer/CommandLineTools/SDKs/MacOSX.sdk/usr/include
  $<BUILD_INTERFACE:${CMAKE_CURRENT_SOURCE_DIR}/libhacl/include>
  $<INSTALL_INTERFACE:libhacl/include>  # <prefix>/libhacl/include
)


option(CAN_USE_VECTOR_INTRINSICS "Use vector intrinsics for math functions if available"     ON)
option(USE_32BIT_MATH            "Use the 32 bit optimized math impl"                        OFF)

# HACL includes processor optimized vectors where possible
if(CAN_USE_VECTOR_INTRINSICS)
    message("++ Using Vector Intrinsics")
    add_compile_definitions(HACL_CAN_COMPILE_VEC128)
    add_compile_definitions(HACL_CAN_COMPILE_VEC256)
endif()

if(PROJECT_SOURCE_DIR STREQUAL PROJECT_BINARY_DIR)
    message(FATAL_ERROR "In-source builds not allowed. Please make a new /build/ directory and run CMake from there.")
endif()

# Usage: 
# - mkdir build
# - cd build
# - cmake -DCMAKE_BUILD_TYPE=Release ..
# - make
# Results in build/libhacl.a 
