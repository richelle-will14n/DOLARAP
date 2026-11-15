package com.aistudio.dolarap.di

import org.koin.core.module.Module

/**
 * Lista de módulos Koin registrados na inicialização da aplicação.
 * Etapa E02: databaseModule (Room).
 * Etapa E03: networkModule (Retrofit, OkHttp, Moshi e AwesomeAPI).
 */
val appModules: List<Module> = listOf(databaseModule, networkModule)
