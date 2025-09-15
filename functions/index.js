const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Trigger when a new message document is created in `messages` collection
exports.sendNotificationOnNewMessage = functions.firestore
  .document('messages/{messageId}')
  .onCreate(async (snap, context) => {
    const message = snap.data();
    const senderEmail = message.senderEmail || 'Alguien';
    const text = message.text || '';

    // For simplicity: send notification to all stored tokens
    const tokensSnapshot = await admin.firestore().collection('fcm_tokens').get();
    const tokens = [];
    tokensSnapshot.forEach(doc => {
      const data = doc.data();
      if (data && data.token) tokens.push(data.token);
    });

    if (tokens.length === 0) return null;

    const payload = {
      notification: {
        title: `Nuevo mensaje de ${senderEmail}`,
        body: text ? text : 'Imagen enviada',
      },
      data: {
        messageId: context.params.messageId
      }
    };

    try {
      const response = await admin.messaging().sendToDevice(tokens, payload);
      console.log('Notifications sent:', response.successCount);
    } catch (err) {
      console.error('Error sending notifications', err);
    }
    return null;
  });
