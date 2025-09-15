# MobileFirstProject — Chat con Firebase

Este proyecto contiene una app Android mínima para chat usando Firebase (Auth, Firestore, Storage, Messaging).

Resumen de lo incluido:

- Autenticación por email/password (LoginActivity, RegisterActivity)
- Chat en tiempo real usando Firestore (ChatActivity, Message, ChatAdapter)
- Envío de imágenes con Firebase Storage
- Servicio FCM para recibir notificaciones (MyFirebaseMessagingService)

¿Qué falta (pasos manuales)?

1. Añadir `google-services.json` en `app/`.
   - Crea un proyecto en Firebase Console, añade una app Android con `applicationId` = `mobile.app`.
   - Descarga `google-services.json` y coloca en la carpeta `app/`.

2. Habilitar en Firebase Console:
   - Authentication -> Email/Password.
   - Firestore (modo de pruebas o reglas adecuadas para el proyecto académico).
   - Storage (reglas de pruebas o personalizadas).
   - Cloud Messaging (para notificaciones push).

3. Enviar notificaciones push:
   - Para que las notificaciones lleguen cuando hay nuevos mensajes, necesitas un servidor o Cloud Function que escuche la colección `messages` y envíe un mensaje FCM al destinatario(s).
   - Ejemplo: una Cloud Function que dispara al crear un documento en `messages` y envía un `notification` a tokens almacenados por usuario.

Cómo compilar y ejecutar (Windows, PowerShell):

- Abre una terminal en la raíz del proyecto.
- Ejecuta `./gradlew assembleDebug` o usa Android Studio para sincronizar Gradle y ejecutar en un emulador/dispositivo.

Notas sobre privacidad y seguridad:

- Recomendado cambiar reglas de Firestore y Storage para producción.
- Para cumplir requisitos de proyecto académico, el modo de pruebas puede ser suficiente, pero documenta las reglas que usaste.

Limitaciones y mejoras opcionales:

- Chats grupales no implementados.
- Estado en línea no implementado.
- Cifrado de mensajes no implementado.

Contacto:

Si quieres que implemente Cloud Functions de ejemplo o la gestión de tokens FCM por usuario, dime y lo agrego.
