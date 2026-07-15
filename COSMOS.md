# Guía de Conexión a Cosmos DB - ImpactX Movil Workspace

Esta guía contiene la configuración de conexión y las pautas para integrar **Azure Cosmos DB NoSQL** en el repositorio del equipo móvil.

## Credenciales de Conexión (Desarrollo / Pruebas)

* **Account Endpoint:** `https://impactx-db-west-final.documents.azure.com:443/`
* **Account Key (Lectura y Escritura):** `<REPLACE_WITH_YOUR_COSMOS_KEY>`
* **Base de Datos Principal:** `ImpactX-Data`
* **Base de Datos Temporal/Test:** `TestDatabase`

---

## ⚠️ IMPORTANTE: Seguridad en Aplicaciones Móviles

> [!CAUTION]
> **NUNCA introduzcas la clave de acceso directo (Account Key) dentro del código de la aplicación móvil (React Native, Flutter, Swift, Kotlin).**
> Si incrustas la clave en el código del cliente, cualquier usuario malintencionado podría decompilar la aplicación (`.apk` o `.ipa`), extraer la clave y tener control absoluto (lectura, escritura y borrado) sobre toda la base de datos de Cosmos DB.

### Arquitectura Recomendada:
Para interactuar con Cosmos DB desde la aplicación móvil:
1. La aplicación móvil debe enviar peticiones HTTP a **`ImpactX-backend-apis`**.
2. El backend (que se ejecuta de forma segura en un servidor/cloud) valida la petición.
3. El backend se conecta a Cosmos DB utilizando esta Key, realiza la operación y le devuelve los resultados depurados a la aplicación móvil.

---

## Cómo usar Cosmos DB Studio para Validaciones Locales

Para ver o modificar los datos de Cosmos DB desde tu laptop sin entrar a Azure Portal:

1. Descarga e instala **Cosmos DB Studio**.
2. Crea una nueva conexión ingresando los siguientes datos:
   * **Name:** `ImpactX`
   * **Endpoint:** `https://impactx-db-west-final.documents.azure.com:443/`
   * **Key:** `<REPLACE_WITH_YOUR_COSMOS_KEY>`
   * **Serverless:** Desmarcado
   * **Folder:** En blanco
3. Haz clic en **OK**.
4. Haz doble clic en el contenedor (por ejemplo, `TestContainer` o el que crees en `ImpactX-Data`).
5. En la ventana central escribe:
   ```sql
   SELECT * FROM c
   ```
6. Haz clic en el botón de **Play (Triángulo Negro)** para ver los datos en la sección inferior.
