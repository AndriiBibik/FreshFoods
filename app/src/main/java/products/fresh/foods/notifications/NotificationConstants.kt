package products.fresh.foods.notifications

class NotificationConstants {
    companion object {
        const val TITLE_KEY = "title"
        const val EXPIRY_DATE_KEY = "expiry_date"
        const val EXPIRY_DATE_ID_KEY = "expiry_date_id"
        const val IMAGE_PATH_KEY = "image_path"
        const val NOTIFICATION_ID_KEY = "notification_id"
        const val ALL_NOTIFICATIONS_KEY = "all_notifications"
        const val GROUP = "fresh_foods_group"
        const val EXPIRY_DATES_IDS_TO_DELETE = "expiry_date_to_delete"

        // notification options
        const val NOTIFICATION_SHARED_PREFERENCES = "notification_shared_preferences"
        const val NOTIFICATION_HOUR_KEY = "notification_hour_key"
        const val NOTIFICATION_DEFAULT_HOUR = 9
        const val NOTIFICATION_DAYS_BEFORE_KEY = "notification_days_before_key"
        const val DIVIDER = "|"
        const val TRUE = true
        const val FALSE = false
        const val NOTIFICATION_DEFAULT_DAYS_BEFORE =
            "${TRUE}${DIVIDER}" + //0
                    "${TRUE}${DIVIDER}" + //1
                    "${FALSE}${DIVIDER}" + //2
                    "${TRUE}${DIVIDER}" + //3
                    "${FALSE}${DIVIDER}" + //4
                    "${TRUE}${DIVIDER}" + //5
                    "${FALSE}${DIVIDER}" + //6
                    "${FALSE}" //7
        const val MAX_DAYS_BEFORE = 8

        // general options dialog (from menu)
        const val GENERAL_OPTIONS_LAYOUT = 0
        const val NOTIFICATION_OPTIONS_LAYOUT = 1

        // default delete days after product expiry
        const val DEFAULT_DELETE_DAYS = 7
        const val DELETE_DAYS_KEY = "delete_days"
        const val NEVER_DELETE_CHECKBOX_KEY = "never_delete_checkbox"

        // to delete notifications if shown
        const val NOTIFICATIONS_TO_DELETE_KEY = "notifications_to_delete"
    }
}