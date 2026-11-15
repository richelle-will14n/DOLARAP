package com.aistudio.dolarap.di

import org.koin.core.module.Module

/**
 * Lista de módulos Koin registrados na inicialização da aplicação.
 * Etapa E02: inclui o módulo de persistência local Room (Database e DAOs).
 */
val appModules: List<Module> = listOf(databaseModule)
