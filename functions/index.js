const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

// Trigger when a new message is created in a chat
exports.sendNotificationOnNewMessage = onDocumentCreated(
    "chats/{chatId}/messages/{messageId}",
    async (event) => {
      const message = event.data.data();
      const chatId = event.params.chatId;
      const senderId = message.senderId;

      try {
        const db = getFirestore();

        // Get chat document to find the recipient
        const chatDoc = await db.collection("chats").doc(chatId).get();
        if (!chatDoc.exists) {
          console.log("Chat not found");
          return null;
        }

        const chatData = chatDoc.data();
        const participants = chatData.participants || [];

        // Find the recipient (not the sender)
        const recipientId = participants.find((id) => id !== senderId);
        if (!recipientId) {
          console.log("No recipient found");
          return null;
        }

        // Get recipient's FCM token
        const recipientDoc = await db.collection("users")
            .doc(recipientId).get();
        if (!recipientDoc.exists) {
          console.log("Recipient user not found");
          return null;
        }

        const recipientData = recipientDoc.data();
        const fcmToken = recipientData.fcmToken;

        if (!fcmToken) {
          console.log("Recipient has no FCM token");
          return null;
        }

        // Check if recipient is online - if so, don't send notification
        if (recipientData.online === true) {
          console.log("Recipient is online, skipping notification");
          return null;
        }

        // Get sender's info
        const senderDoc = await db.collection("users").doc(senderId).get();
        const senderEmail = senderDoc.exists ?
         senderDoc.data().email : "Alguien";
        const senderName = senderEmail.split("@")[0];

        // Prepare message body
        let body = message.text || "";
        if (message.imageBase64) {
          body = "📷 Foto";
        } else if (message.imageUrl) {
          body = "📷 Foto";
        }

        // Get contact name if exists
        let contactName = senderName;
        const contactsSnapshot = await db
            .collection("users")
            .doc(recipientId)
            .collection("contacts")
            .where("userId", "==", senderId)
            .limit(1)
            .get();

        if (!contactsSnapshot.empty) {
          const contactData = contactsSnapshot.docs[0].data();
          contactName = contactData.nickname || senderName;
        }

        // Send notification
        const payload = {
          data: {
            title: contactName,
            body: body,
            chatId: chatId,
            otherUserId: senderId,
            otherUserEmail: senderEmail,
            contactName: contactName,
          },
        };

        const response = await getMessaging().send({
          token: fcmToken,
          data: payload.data,
        });
        console.log("Notification sent successfully:", response);

        return null;
      } catch (error) {
        console.error("Error sending notification:", error);
        return null;
      }
    },
);
