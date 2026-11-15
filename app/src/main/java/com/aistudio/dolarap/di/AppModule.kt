package com.aistudio.dolarap.di

import org.koin.core.module.Module

/**
 * Lista de módulos Koin registrados na inicialização da aplicação.
 * Etapa E02: databaseModule (Room).
 * Etapa E03: networkModule (Retrofit, OkHttp, Moshi e AwesomeAPI).
 * Etapa E04: dataModule e domainModule (Repositórios SSOT, BigDecimal Converter e UseCases).
 */
val appModules: List<Module> = listOf(databaseModule, networkModule, dataModule, domainModule)
