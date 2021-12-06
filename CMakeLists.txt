cmake_minimum_required(VERSION 3.21)

project(hacl)

ADD_LIBRARY(hacl STATIC
    libhacl/src/Hacl_Bignum.c
    libhacl/src/Hacl_Bignum256.c
    libhacl/src/Hacl_Bignum256_32.c
    libhacl/src/Hacl_Bignum4096.c
    libhacl/src/Hacl_Bignum4096_32.c
    libhacl/src/Hacl_HMAC.c
    libhacl/src/Hacl_HMAC_DRBG.c
    libhacl/src/Hacl_Hash.c
    libhacl/src/Hacl_Streaming_SHA2.c
    libhacl/src/Lib_Memzero.c
    libhacl/src/Lib_Memzero0.c
    libhacl/src/Lib_RandomBuffer_System.c
)

target_include_directories(hacl PUBLIC
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
