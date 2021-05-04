/*
 * Copyright 2021 JellyBrick, nkgcp, and all its contributor. All rights reserved.
 *
 * Licensed undr the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions andlimitations under the License.
 */
package be.zvz.koreanbots

import be.zvz.koreanbots.dto.Bot
import be.zvz.koreanbots.dto.ResponseWrapper
import be.zvz.koreanbots.dto.ServersUpdate
import be.zvz.koreanbots.dto.User
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Headers
import com.github.kittinunf.fuel.jackson.objectBody
import com.github.kittinunf.fuel.jackson.responseObject
import com.github.kittinunf.result.Result
import com.github.kittinunf.result.getOrElse

class KoreanBots @JvmOverloads constructor(
    private val token: String,
    private val mapper: ObjectMapper = JsonMapper()
        .registerKotlinModule()
        .enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
        .enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING),
    private val fuelManager: FuelManager = FuelManager()
) {
    init {
        fuelManager.basePath = KoreanBotsInfo.API_BASE_URL
        fuelManager.baseHeaders = mapOf(
            Headers.USER_AGENT to "Kotlin SDK(${KoreanBotsInfo.VERSION}, ${KoreanBotsInfo.GITHUB_URL})"
        )
    }

    /**
     * [Bot] 봇 정보를 받아옵니다.
     * @param id 봇의 ID 입니다.
     *
     * @throws [RequestFailedException] 요청이 실패한 경우 [RequestFailedException]을 던집니다.
     * @throws [AssertionError] 요청이 성공했지만 아무 응답도 반환하지 않은 경우 [AssertionError]을 던집니다.
     *
     * @return [Bot] 봇 정보를 반환합니다.
     */
    @Throws(RequestFailedException::class, AssertionError::class)
    fun getBotInfo(id: String) = handleResponse(
        fuelManager
            .get("/bots/$id")
            .responseObject<ResponseWrapper<Bot>>(mapper = mapper)
            .third
    )
        ?: throw AssertionError("Request Success, but Data Doesn't Exist") // This may not occur, but in case of server api error

    /**
     * 봇 서버 수를 업데이트합니다.
     * @param id 봇의 ID 입니다.
     * @param servers 서버 수 입니다.
     *
     * @throws [RequestFailedException] 요청이 실패한 경우 [RequestFailedException]을 던집니다.
     *
     * @return [Bot] 봇 정보를 반환합니다.
     */
    fun updateBotServers(id: String, servers: Int) {
        handleResponse(
            fuelManager
                .post("/bots/$id/stats")
                .header(Headers.AUTHORIZATION, token)
                .objectBody(ServersUpdate(servers), mapper = mapper)
                .responseObject<ResponseWrapper<Unit>>(mapper = mapper)
                .third
        )
    }

    /**
     * [User] 유저 정보를 받아옵니다.
     * @param id 유저의 ID 입니다.
     *
     * @throws [RequestFailedException] 요청이 실패한 경우 [RequestFailedException]을 던집니다.
     * @throws [AssertionError] 요청이 성공했지만 아무 응답도 반환하지 않은 경우 [AssertionError]을 던집니다.
     *
     * @return [User] 유저 정보를 반환합니다.
     */
    @Throws(RequestFailedException::class, AssertionError::class)
    fun getUserInfo(id: String) = handleResponse(
        fuelManager
            .get("/users/$id")
            .responseObject<ResponseWrapper<User>>(mapper = mapper)
            .third
    )
        ?: throw AssertionError("Request Success, but Data Doesn't Exist") // This may not occur, but in case of server api error

    private fun <T> handleResponse(result: Result<ResponseWrapper<T>, FuelError>): T? =
        result.getOrElse { throw RequestFailedException(it.message.orEmpty()) }
            .apply {
                if (this.code !in 200..299)
                    throw RequestFailedException("API responded ${this.code}: ${this.message}")
            }
            .data
}
