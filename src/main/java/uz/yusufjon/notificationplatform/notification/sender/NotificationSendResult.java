package uz.yusufjon.notificationplatform.notification.sender;

public record NotificationSendResult(
        boolean success,
        String providerResponse,
        String errorMessage
) {

    public static NotificationSendResult success(String providerResponse) {
        return new NotificationSendResult(true, providerResponse, null);
    }

    public static NotificationSendResult failure(String errorMessage) {
        return new NotificationSendResult(false, null, errorMessage);
    }
}
