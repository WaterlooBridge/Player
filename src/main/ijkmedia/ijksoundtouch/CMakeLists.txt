cmake_minimum_required(VERSION 3.6)


include_directories(${CMAKE_CURRENT_LIST_DIR})
include_directories(${CMAKE_CURRENT_LIST_DIR}/..)
include_directories(${CMAKE_CURRENT_LIST_DIR}/include/)

set(SOUND_THOCH_SOURCES
        source/SoundTouch/AAFilter.cpp
        source/SoundTouch/FIFOSampleBuffer.cpp
        source/SoundTouch/FIRFilter.cpp
        source/SoundTouch/cpu_detect_x86.cpp
        source/SoundTouch/sse_optimized.cpp
        source/SoundTouch/RateTransposer.cpp
        source/SoundTouch/InterpolateCubic.cpp
        source/SoundTouch/InterpolateLinear.cpp
        source/SoundTouch/InterpolateShannon.cpp
        source/SoundTouch/TDStretch.cpp
        source/SoundTouch/BPMDetect.cpp
        source/SoundTouch/PeakFinder.cpp
        source/SoundTouch/SoundTouch.cpp
        source/SoundTouch/mmx_optimized.cpp
        ijksoundtouch_wrap.cpp
        )

add_library(ijksoundtouch STATIC ${SOUND_THOCH_SOURCES})