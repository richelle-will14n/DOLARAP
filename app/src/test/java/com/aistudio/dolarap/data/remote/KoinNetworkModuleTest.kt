package com.aistudio.dolarap.data.remote

import com.aistudio.dolarap.BuildConfig
import com.aistudio.dolarap.data.local.DolarapDatabase
import com.aistudio.dolarap.data.remote.mapper.QuoteRemoteMapper
import com.aistudio.dolarap.data.remote.parser.AvailableCatalogParser
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Retrofit

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class KoinNetworkModuleTest {

  @After
  fun tearDown() {
    GlobalContext.getOrNull()?.getOrNull<DolarapDatabase>()?.close()
  }

  @Test
  fun `Koin resolves network stack and AwesomeApiRemoteDataSource with AwesomeAPI baseUrl and auth interceptor`() {
    val koin = GlobalContext.get()

    val moshi = koin.get<Moshi>()
    val okHttp = koin.get<OkHttpClient>()
    val retrofit = koin.get<Retrofit>()
    val service = koin.get<AwesomeApiService>()
    val mapper = koin.get<QuoteRemoteMapper>()
    val parser = koin.get<AvailableCatalogParser>()
    val dataSource1 = koin.get<AwesomeApiRemoteDataSource>()
    val dataSource2 = koin.get<AwesomeApiRemoteDataSource>()

    assertNotNull(moshi)
    assertNotNull(okHttp)
    assertTrue(okHttp.interceptors.any { it is AwesomeApiAuthInterceptor })
    assertNotNull(BuildConfig.AWESOME_API_KEY)
    assertEquals(AwesomeApiConfig.BASE_URL, retrofit.baseUrl().toString())
    assertNotNull(service)
    assertNotNull(mapper)
    assertNotNull(parser)
    assertSame(dataSource1, dataSource2)
  }
}
