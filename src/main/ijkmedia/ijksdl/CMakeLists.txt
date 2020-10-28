cmake_minimum_required(VERSION 3.6)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)


set(BASE_SOURCES
        ijksdl_class.h
        ijksdl_container.h
        ijksdl_log.h

        ijksdl_misc.h
        ijksdl_stdinc.h
        ijksdl_inc_internal.h
        ijksdl_aout.c
        ijksdl_aout.h
        ijksdl_aout_internal.h
        ijksdl_audio.c
        ijksdl_audio.h
        ijksdl_error.c
        ijksdl_error.h
        ijksdl_mutex.c
        ijksdl_mutex.h
        ijksdl_stdinc.c
        ijksdl_stdinc.h
        ijksdl_thread.c
        ijksdl_thread.h
        ijksdl_timer.c
        ijksdl_timer.h
        ijksdl_vout.c
        ijksdl_vout.h
        ijksdl_vout_internal.h

        ffmpeg/ijksdl_vout_overlay_ffmpeg.c
        ffmpeg/ijksdl_vout_overlay_ffmpeg.h
        ffmpeg/ijksdl_image_convert.h
        ffmpeg/ijksdl_inc_ffmpeg.h
        ffmpeg/abi_all/image_convert.c

        dummy/ijksdl_vout_dummy.c
        dummy/ijksdl_vout_dummy.h
        dummy/ijksdl_dummy.h
        )

set(SDL_ANDROID_EXTRA_SOURCES
        ijksdl_extra_log.c
        ijksdl_extra_log.h
        android/android_audiotrack.c
        android/android_nativewindow.c
        android/ijksdl_android_jni.c
        android/ijksdl_aout_android_audiotrack.c
        android/ijksdl_aout_android_opensles.c
        android/ijksdl_codec_android_mediacodec_dummy.c
        android/ijksdl_codec_android_mediacodec_internal.c
        android/ijksdl_codec_android_mediacodec_java.c
        android/ijksdl_codec_android_mediacodec.c
        android/ijksdl_codec_android_mediadef.c
        android/ijksdl_codec_android_mediaformat_java.c
        android/ijksdl_codec_android_mediaformat.c
        android/ijksdl_vout_android_nativewindow.c
        android/ijksdl_vout_android_surface.c
        android/ijksdl_vout_overlay_android_mediacodec.c
        )

set(GLES2_SOURCES
        ijksdl_egl.c
        gles2/color.c
        gles2/common.c
        gles2/renderer.c
        gles2/renderer_rgb.c
        gles2/renderer_yuv420p.c
        gles2/renderer_yuv444p10le.c
        gles2/shader.c
        gles2/fsh/rgb.fsh.c
        gles2/fsh/yuv420p.fsh.c
        gles2/fsh/yuv444p10le.fsh.c
        gles2/vsh/mvp.vsh.c
        )

set(IJK_SDL_SOURCES ${BASE_SOURCES} ${SDL_ANDROID_EXTRA_SOURCES} ${GLES2_SOURCES})

add_library(ijksdl SHARED ${IJK_SDL_SOURCES})
set(IJKSDL_LIB_NAME ijksdl)

target_compile_options(${IJKSDL_LIB_NAME} PUBLIC "-Werror")

target_include_directories(${IJKSDL_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR})
target_include_directories(${IJKSDL_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/..)
target_include_directories(${IJKSDL_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../include)
target_include_directories(${IJKSDL_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../ijkyuv/include)
target_include_directories(${IJKSDL_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../ijkj4a)

target_link_libraries(${IJKSDL_LIB_NAME} ijkffmpeg)
target_link_libraries(${IJKSDL_LIB_NAME} ijkj4a)
target_link_libraries(${IJKSDL_LIB_NAME} yuv)
target_link_libraries(${IJKSDL_LIB_NAME} log android OpenSLES EGL GLESv2)