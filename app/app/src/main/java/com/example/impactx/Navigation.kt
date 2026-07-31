package com.example.impactx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import com.example.impactx.data.local.AppDatabase
import com.example.impactx.data.remote.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import com.example.impactx.ui.screens.*

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  var activePlan by remember { mutableStateOf("Básico") }
  var userName by remember { mutableStateOf("Alberto Zepeda") }
  val userId by remember { mutableStateOf("IX-9831-AZ") }
  val backStack = rememberNavBackStack(Splash)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(
            onTimeout = { hasSession, sessionUsername, sessionPlan ->
              backStack.removeLastOrNull() // remove Splash
              if (hasSession) {
                userName = sessionUsername
                activePlan = sessionPlan
                backStack.add(Home)
              } else {
                backStack.add(Welcome)
              }
            }
          )
        }
        entry<Welcome> {
          WelcomeScreen(
            onNavigateToLogin = { backStack.add(Login) },
            onNavigateToRegister = { backStack.add(Register) }
          )
        }
        entry<Login> {
          LoginScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onLoginSuccess = { backStack.add(Home) },
            onNavigateToRegister = { backStack.add(Register) }
          )
        }
        entry<Register> {
          RegisterScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onRegisterSuccess = { registeredName ->
              userName = registeredName
              backStack.add(Home)
            },
            onNavigateToLogin = { backStack.add(Login) }
          )
        }
        entry<Home> {
          HomeScreen(
            currentPlan = activePlan,
            userName = userName,
            userId = userId,
            onUserNameChange = { userName = it },
            onNavigateToMedical = { backStack.add(Medical) },
            onNavigateToVehicle = { backStack.add(Vehicle) },
            onNavigateToContacts = { backStack.add(Contacts) },
            onNavigateToPlans = { backStack.add(Plans) },
            onNavigateToWearableSync = { backStack.add(WearableSync) },
            onLogout = { 
              coroutineScope.launch {
                val db = AppDatabase.getDatabase(context)
                val session = withContext(Dispatchers.IO) { db.sessionDao().session }
                if (session != null) {
                  try {
                    val apiService = ApiClient.getApiService(context)
                    apiService.logout("Bearer ${session.accessToken}")
                  } catch (e: Exception) {
                    // Safe ignore network error on logout
                  }
                }
                withContext(Dispatchers.IO) { db.sessionDao().clearSession() }
                backStack.removeLastOrNull()
                backStack.add(Welcome)
              }
            }
          )
        }
        entry<Medical> {
          MedicalScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Vehicle> {
          VehicleScreen(
            currentPlan = activePlan,
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToPlans = { backStack.add(Plans) }
          )
        }
        entry<Contacts> {
          ContactsScreen(
            currentPlan = activePlan,
            onNavigateBack = { backStack.removeLastOrNull() },
            onNavigateToPlans = { backStack.add(Plans) }
          )
        }
        entry<ActiveTrip> {
          ActiveTripScreen(
            currentPlan = activePlan,
            onTriggerSos = { backStack.add(EmergencyChat) },
            onFinishTrip = { backStack.removeLastOrNull() },
            onNavigateToPlans = { backStack.add(Plans) }
          )
        }
        entry<Plans> {
          PlansScreen(
            currentPlan = activePlan,
            onNavigateBack = { backStack.removeLastOrNull() },
            onPlanSelected = { activePlan = it }
          )
        }
        entry<EmergencyChat> {
          EmergencyChatScreen(
            onCloseChat = { backStack.removeLastOrNull() }
          )
        }
        entry<WearableSync> {
          WearableSyncScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
