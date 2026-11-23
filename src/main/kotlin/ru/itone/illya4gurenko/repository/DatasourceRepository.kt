package ru.itone.illya4gurenko.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.itone.illya4gurenko.entity.AppDispatcherDatasource
import java.util.UUID

/**
 * репозиторий для работы с источниками данных счетов
 */
@Repository
interface DatasourceRepository : JpaRepository<AppDispatcherDatasource, UUID> {
    fun findAllByAccountIn(accounts: Collection<String>): List<AppDispatcherDatasource>
    fun findAllByAccountInAndStatus(accounts: Collection<String>, status: String): List<AppDispatcherDatasource>
}
