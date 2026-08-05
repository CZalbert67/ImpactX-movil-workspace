package com.example.impactx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
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

  // Watch for emergency navigation trigger from WearableMessageListenerService (crash detected)
  val emergencyTrigger = WearableManager.triggerEmergencyNav
  LaunchedEffect(emergencyTrigger) {
    if (emergencyTrigger) {
      WearableManager.triggerEmergencyNav = false
      // Navigate to MandarDatos if not already there
      if (backStack.lastOrNull() !is MandarDatos) {
        backStack.add(MandarDatos)
      }
    }
  }

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        val performLogout = {
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
            backStack.clear()
            backStack.add(Welcome)
          }
        }

        entry<Splash> {
          SplashScreen(
            onTimeout = { hasSession, sessionUsername, sessionPlan ->
              val prefs = context.getSharedPreferences("impactx_prefs", android.content.Context.MODE_PRIVATE)
              val onboardingCompleted = prefs.getBoolean("onboarding_completed", false)

              backStack.clear()
              if (!onboardingCompleted) {
                backStack.add(Onboarding)
              } else if (hasSession) {
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
            onLoginSuccess = { loggedInName ->
              userName = loggedInName
              backStack.clear()
              backStack.add(Home)
            },
            onNavigateToRegister = { backStack.add(Register) }
          )
        }
        entry<Register> {
          RegisterScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onRegisterSuccess = { registeredName ->
              userName = registeredName
              backStack.clear()
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
            onNavigateToMessages = { backStack.add(Messages) },
            onNavigateToProfile = { backStack.add(Profile) },
            onNavigateToMandarDatos = { backStack.add(MandarDatos) },
            onLogout = { performLogout() }
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
            onNavigateToPlans = { backStack.add(Plans) },
            onNavigateToMessages = { backStack.add(Messages) }
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
        entry<Messages> {
          MessagesScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Profile> {
          ProfileScreen(
            userName = userName,
            userId = userId,
            currentPlan = activePlan,
            onUserNameChange = { userName = it },
            onNavigateBack = { backStack.removeLastOrNull() },
            onLogout = { performLogout() }
          )
        }
        entry<MandarDatos> {
          MandarDatosScreen(
            onNavigateBack = { backStack.removeLastOrNull() },
            onTriggerSos = {
              // Replace Splash or Home if needed, but here we just add EmergencyChat
              backStack.add(EmergencyChat)
            }
          )
        }
        entry<Onboarding> {
          OnboardingScreen(
            onFinishOnboarding = {
              backStack.clear()
              backStack.add(Welcome)
            }
          )
        }
      },
  )
}
