cmake_minimum_required(VERSION 3.6)

project("Player")

set(CMAKE_C_STANDARD 11)
set(CMAKE_CXX_STANDARD 11)

add_definitions(-DANDROID)

message(STATUS "CMAKE_BUILD_TYPE: ${CMAKE_BUILD_TYPE}")
message(STATUS "ANDROID_PLATFORM_LEVEL:${ANDROID_PLATFORM_LEVEL}")
message(STATUS "ANDROID_ABI:${ANDROID_ABI}")
message(STATUS "PROJECT_SOURCE_DIR ${PROJECT_SOURCE_DIR}")

if (${CMAKE_BUILD_TYPE} STREQUAL "Release")
    add_definitions(-DNDEBUG)
elseif (${CMAKE_BUILD_TYPE} STREQUAL "Debug")
    add_definitions(-DDEBUG)
endif ()

add_compile_options("-Wno-deprecated-declarations")
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

if (${ANDROID_ABI} STREQUAL "arm64-v8a")
    set(IJK_FFMPAG_BUILD_DIR ${PROJECT_SOURCE_DIR}/ffmpeg/arm64-v8a)
elseif (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(IJK_FFMPAG_BUILD_DIR ${PROJECT_SOURCE_DIR}/ffmpeg/armeabi-v7a)
endif ()

add_library(ijkffmpeg SHARED IMPORTED)
set_target_properties( # Specifies the target library.
    ijkffmpeg
    PROPERTIES
    IMPORTED_LOCATION ${IJK_FFMPAG_BUILD_DIR}/libijkffmpeg.so
)

message(STATUE "${CMAKE_LIBRARY_OUTPUT_DIRECTORY} ${PROJECT_BINARY_DIR}")

add_subdirectory(ijkmedia/ijksdl)
add_subdirectory(ijkmedia/ijkplayer)
add_subdirectory(ijkmedia/ijksoundtouch)
add_subdirectory(ijkmedia/ijkyuv)
add_subdirectory(ijkmedia/ijkj4a)