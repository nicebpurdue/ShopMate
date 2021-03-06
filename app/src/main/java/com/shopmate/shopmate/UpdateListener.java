package com.shopmate.shopmate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

/**
 * Utility class for getting realtime updates.
 */
public class UpdateListener {
    private static final String TAG = "UpdateListener";

    private static final String KEY_ACTION = "action";
    private static final String KEY_LIST_TITLE = "listTitle";
    private static final String KEY_INVITE_ID = "inviteId";
    private static final String KEY_SENDER_ID = "senderFbid";
    private static final String KEY_LIST_ID = "listId";
    private static final String KEY_ITEM_ID = "itemId";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_PURCHASE_ID = "purchaseId";

    private static final String ACTION_INVITED = "invited";
    private static final String ACTION_LIST_SHARED = "listShared";
    private static final String ACTION_LIST_MEMBER_LEFT = "listMemberLeft";
    private static final String ACTION_ITEM_ADDED = "itemAdded";
    private static final String ACTION_ITEM_UPDATED = "itemUpdated";
    private static final String ACTION_ITEM_DELETED = "itemDeleted";
    private static final String ACTION_REIMBURSEMENT_REQUESTED = "reimbursementRequested";
    private static final String ACTION_PURCHASE_COMPLETED = "purchaseCompleted";

    private final Context context;
    private final UpdateHandler handler;

    public UpdateListener(Context activity, UpdateHandler handler) {
        this.context = activity;
        this.handler = handler;
    }

    public void register() {
        context.registerReceiver(receiver, new IntentFilter(MessagingService.INTENT_FILTER));
    }

    public void unregister() {
        context.unregisterReceiver(receiver);
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                handleMessage(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to handle message", e);
            }
        }
    };

    private void handleMessage(Intent intent) {
        String action = intent.getStringExtra(KEY_ACTION);
        Log.d(TAG, "Received action " + action + " for " + context.getClass().getName());
        switch (action) {
            case ACTION_INVITED:
                handleInvited(intent);
                break;
            case ACTION_LIST_SHARED:
                handleListShared(intent);
                break;
            case ACTION_LIST_MEMBER_LEFT:
                handleListMemberLeft(intent);
                break;
            case ACTION_ITEM_ADDED:
                handleItemAdded(intent);
                break;
            case ACTION_ITEM_UPDATED:
                handleItemUpdated(intent);
                break;
            case ACTION_ITEM_DELETED:
                handleItemDeleted(intent);
                break;
            case ACTION_REIMBURSEMENT_REQUESTED:
                handleReimbursementRequested(intent);
                break;
            case ACTION_PURCHASE_COMPLETED:
                handlePurchaseCompleted(intent);
                break;
            default:
                Log.w(TAG, "Unsupported action " + action);
                break;
        }
    }

    private void handleInvited(Intent intent) {
        String listTitle = intent.getStringExtra(KEY_LIST_TITLE);
        long inviteId = Long.parseLong(intent.getStringExtra(KEY_INVITE_ID));
        String senderId = intent.getStringExtra(KEY_SENDER_ID);
        handler.onInvited(inviteId, listTitle, senderId);
    }

    private void handleListShared(Intent intent) {
        long listId = Long.parseLong(intent.getStringExtra(KEY_LIST_ID));
        handler.onListShared(listId);
    }

    private void handleListMemberLeft(Intent intent) {
        long listId = Long.parseLong(intent.getStringExtra(KEY_LIST_ID));
        String userId = intent.getStringExtra(KEY_USER_ID);
        handler.onListMemberLeft(listId, userId);
    }

    private void handleItemAdded(Intent intent) {
        long listId = Long.parseLong(intent.getStringExtra(KEY_LIST_ID));
        long itemId = Long.parseLong(intent.getStringExtra(KEY_ITEM_ID));
        handler.onItemAdded(listId, itemId);
    }

    private void handleItemUpdated(Intent intent) {
        long itemId = Long.parseLong(intent.getStringExtra(KEY_ITEM_ID));
        handler.onItemUpdated(itemId);
    }

    private void handleItemDeleted(Intent intent) {
        long itemId = Long.parseLong(intent.getStringExtra(KEY_ITEM_ID));
        handler.onItemDeleted(itemId);
    }

    private void handleReimbursementRequested(Intent intent) {
        long purchaseId = Long.parseLong(intent.getStringExtra(KEY_PURCHASE_ID));
        handler.onReimbursementRequested(purchaseId);
    }

    private void handlePurchaseCompleted(Intent intent) {
        long purchaseId = Long.parseLong(intent.getStringExtra(KEY_PURCHASE_ID));
        handler.onPurchaseCompleted(purchaseId);
    }
}
