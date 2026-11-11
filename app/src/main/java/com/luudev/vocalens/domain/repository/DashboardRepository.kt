package com.luudev.vocalens.domain.repository

import com.luudev.vocalens.domain.model.Dashboard

interface DashboardRepository {
    fun getDashboard(): Dashboard
}
