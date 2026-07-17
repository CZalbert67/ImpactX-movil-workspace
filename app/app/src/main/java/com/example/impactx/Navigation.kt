package com.example.impactx

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.impactx.ui.screens.*

@Composable
fun MainNavigation() {
  var activePlan by remember { mutableStateOf("Básico") }
  val backStack = rememberNavBackStack(Splash)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Splash> {
          SplashScreen(
            onTimeout = {
              backStack.removeLastOrNull() // remove Splash
              backStack.add(Welcome)      // go to Welcome
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
            onRegisterSuccess = { backStack.add(Home) },
            onNavigateToLogin = { backStack.add(Login) }
          )
        }
        entry<Home> {
          HomeScreen(
            currentPlan = activePlan,
            onNavigateToMedical = { backStack.add(Medical) },
            onNavigateToVehicle = { backStack.add(Vehicle) },
            onNavigateToContacts = { backStack.add(Contacts) },
            onNavigateToPlans = { backStack.add(Plans) },
            onNavigateToWearableSync = { backStack.add(WearableSync) },
            onLogout = { 
              backStack.removeLastOrNull() // go back to welcome
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
