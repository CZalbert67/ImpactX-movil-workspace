package com.example.impactx

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.impactx.ui.screens.*

@Composable
fun MainNavigation() {
  val backStack = rememberNavBackStack(Welcome)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
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
            onNavigateToMedical = { backStack.add(Medical) },
            onNavigateToVehicle = { backStack.add(Vehicle) },
            onNavigateToContacts = { backStack.add(Contacts) },
            onStartTrip = { backStack.add(ActiveTrip) },
            onLogout = { backStack.removeLastOrNull() } // go back to login/welcome
          )
        }
        entry<Medical> {
          MedicalScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Vehicle> {
          VehicleScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<Contacts> {
          ContactsScreen(
            onNavigateBack = { backStack.removeLastOrNull() }
          )
        }
        entry<ActiveTrip> {
          ActiveTripScreen(
            onFinishTrip = { backStack.removeLastOrNull() }
          )
        }
      },
  )
}
