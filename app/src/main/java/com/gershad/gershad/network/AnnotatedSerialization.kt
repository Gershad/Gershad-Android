package com.gershad.gershad.network

/**
 * Skip Serialization annotation for fields
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SkipSerialization

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FIELD)
annotation class SkipDeserialization