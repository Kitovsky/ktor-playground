package com.lonerx

import com.lonerx.fortunes.FortunesProvider
import com.lonerx.fortunes.FortunesKafkaPublisher
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val fortunesModule = module(createdAtStart = true) {
    singleOf(::FortunesProvider)
    singleOf(::FortunesKafkaPublisher)
}