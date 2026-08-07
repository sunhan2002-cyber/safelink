package com.safelink.app.ui.screens.support

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.safelink.app.ui.components.SafeLinkCard
import com.safelink.app.ui.components.SafeLinkOutlinedButton
import com.safelink.app.ui.components.SafeLinkPrimaryButton
import com.safelink.app.ui.components.SafeLinkTopBar
import com.safelink.app.ui.navigation.Screen
import com.safelink.app.ui.theme.BrandBlue
import com.safelink.app.ui.theme.BrandBlueLight

/** 지원 기관 상세 (Figma 20:1236) — 기관 정보 + 전화 연결 (Task 5.4) */
@Composable
fun SupportDetailScreen(navController: NavHostController, institutionId: String) {
    val institution = dummyInstitutions.find { it.id == institutionId } ?: dummyInstitutions.first()

    Column(modifier = Modifier.fillMaxSize()) {
        SafeLinkTopBar(title = "기관 정보", onBack = { navController.popBackStack() })

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 기관 헤더
            SafeLinkCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(BrandBlueLight, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AccountBalance,
                            contentDescription = null,
                            tint = BrandBlue
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(text = institution.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = institution.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 추천 이유
            SafeLinkCard(containerColor = BrandBlueLight) {
                Text(text = "추천 이유", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "현재 감지된 상황에 도움을 받을 수 있는 기관입니다.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // 정보 리스트
            SafeLinkCard {
                InfoRow(icon = Icons.Filled.Call, label = "전화번호", value = institution.phone)
                Spacer(modifier = Modifier.size(12.dp))
                InfoRow(icon = Icons.Filled.Schedule, label = "운영시간", value = institution.hours)
                Spacer(modifier = Modifier.size(12.dp))
                InfoRow(icon = Icons.Filled.Groups, label = "지원 대상", value = institution.target)
            }

            TextButton(onClick = {
                navController.navigate(Screen.ApplicationGuide.createRoute(institution.id))
            }) {
                Text("신청 절차 안내 보기 →")
            }
        }

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SafeLinkPrimaryButton(text = "전화하기", onClick = {
                // TODO: ACTION_DIAL Intent (Task 5.7)
            })
            SafeLinkOutlinedButton(text = "홈페이지 이동", onClick = {
                // TODO: 브라우저 Intent
            })
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp)
        )
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
    }
}
