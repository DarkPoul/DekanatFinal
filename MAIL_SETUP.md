## Налаштування поштового модуля (.env)

Додайте/уточніть наступні змінні в `.env` (використовується SMTP для відправки та IMAP для читання):

```
# SMTP (вже використовувалося раніше)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=dekanat.support@ntu.edu.ua
MAIL_PASSWORD=<app_password>
MAIL_DEFAULT_FROM=dekanat.support@ntu.edu.ua

# IMAP (нове)
MAIL_IMAP_HOST=imap.gmail.com
MAIL_IMAP_PORT=993
MAIL_IMAP_USERNAME=dekanat.support@ntu.edu.ua
MAIL_IMAP_PASSWORD=<app_password>
MAIL_IMAP_INBOX=INBOX
MAIL_IMAP_SENT=[Gmail]/Sent Mail
MAIL_IMAP_SSL=true

# Інтервал опитування (мс)
MAIL_SYNC_INTERVAL_MS=60000
```

> Використовуйте пароль додатка Gmail, а не основний пароль. Якщо папка вихідних листів має іншу назву, змініть `MAIL_IMAP_SENT`.
