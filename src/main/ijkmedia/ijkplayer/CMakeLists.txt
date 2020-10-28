cmake_minimum_required(VERSION 3.6)

if (${ANDROID_ABI} STREQUAL "armeabi-v7a")
    set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -mfloat-abi=soft")
endif ()

set(BUILD_SOURCES
        ff_fferror.h
        ff_ffinc.h
        ff_ffmsg.h
        ff_ffmsg_queue.h

        ff_cmdutils.c
        ff_cmdutils.h
        ff_ffplay.c
        ff_ffplay.h
        ff_ffplay_debug.h
        ff_ffplay_def.h
        ff_ffplay_options.h

        ff_ffpipeline.c
        ff_ffpipeline.h
        ff_ffpipenode.c
        ff_ffpipenode.h
        ijkmeta.c
        ijkmeta.h

        ijkplayer.c
        ijkplayer.h
        ijkplayer_internal.h

        pipeline/ffpipeline_ffplay.c
        pipeline/ffpipeline_ffplay.h
        pipeline/ffpipenode_ffplay_vdec.c
        pipeline/ffpipenode_ffplay_vdec.h

        ijkavformat/allformats.c
        ijkavformat/ijklivehook.c
        ijkavformat/ijkmediadatasource.c
        ijkavformat/ijkio.c
        ijkavformat/ijkiomanager.c
        ijkavformat/ijkiocache.c
        ijkavformat/ijkioffio.c
        ijkavformat/ijkioprotocol.c
        ijkavformat/ijkioapplication.c
        ijkavformat/ijkiourlhook.c
        ijkavformat/ijkasync.c
        ijkavformat/ijkurlhook.c
        ijkavformat/ijklongurl.c
        ijkavformat/ijksegment.c

        ijkavutil/ijkdict.c
        ijkavutil/ijkutils.c
        ijkavutil/ijkthreadpool.c
        ijkavutil/ijktree.c
        ijkavutil/ijkfifo.c
        ijkavutil/ijkstl.cpp
        ijkavutil/md5.c
        )

set(ANDROID_IJKPLAYER_SOURCES
        ijkavformat/ijkioandroidio.c
        android/ffmpeg_api_jni.c
        android/ijkplayer_android.c
        android/ijkplayer_jni.c
        android/pipeline/ffpipeline_android.c
        android/pipeline/ffpipenode_android_mediacodec_vdec.c
        )

set(BUILD_SOURCES ${BUILD_SOURCES} ${ANDROID_IJKPLAYER_SOURCES})

add_library(ijkplayer SHARED ${BUILD_SOURCES})
set(IJKPLAYER_LIB_NAME ijkplayer)

target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR})
target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/..)
target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../include)

target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ijkavformat)
target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ijkavutil)
target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE pipeline)
target_include_directories(${IJKPLAYER_LIB_NAME} PRIVATE ${CMAKE_CURRENT_LIST_DIR}/../ijkj4a)

target_link_libraries(${IJKPLAYER_LIB_NAME} ijksdl)
target_link_libraries(${IJKPLAYER_LIB_NAME} ijksoundtouch)
target_link_libraries(${IJKPLAYER_LIB_NAME} ijkffmpeg)
target_link_libraries(${IJKPLAYER_LIB_NAME} log)
target_link_libraries(${IJKPLAYER_LIB_NAME} android)