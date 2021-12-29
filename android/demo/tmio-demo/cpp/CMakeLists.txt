cmake_minimum_required(VERSION 3.10)

project("tmio-sample")

option(STANDALONE_DEMO "Build demo out of tree" OFF)

message(STATUS "STANDALONE_DEMO ${STANDALONE_DEMO}")
if (STANDALONE_DEMO)
    add_library(tmio SHARED IMPORTED GLOBAL)
    set_target_properties(tmio PROPERTIES
            IMPORTED_LOCATION ${CMAKE_CURRENT_SOURCE_DIR}/../../../sdk/lib/${ANDROID_ABI}/libtmio.so)
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../sdk/include)
else ()
    include_directories(${CMAKE_CURRENT_SOURCE_DIR}/../../../include)
    add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../../../ ${CMAKE_CURRENT_BINARY_DIR}/tmio_lib)
endif ()

add_library(tmiojni
        SHARED
        tmiojni.cpp)

target_link_libraries(tmiojni
        tmio)