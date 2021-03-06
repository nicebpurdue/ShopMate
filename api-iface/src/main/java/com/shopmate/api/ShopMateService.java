package com.shopmate.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.shopmate.api.model.item.ShoppingListItem;
import com.shopmate.api.model.item.ShoppingListItemUpdate;
import com.shopmate.api.model.list.ShoppingList;
import com.shopmate.api.model.purchase.ShoppingItemPurchase;
import com.shopmate.api.model.result.CreateShoppingListItemResult;
import com.shopmate.api.model.result.CreateShoppingListResult;
import com.shopmate.api.model.result.GetAllInvitesResult;
import com.shopmate.api.model.result.GetAllShoppingListsResult;
import com.shopmate.api.model.result.SendInviteResult;

/**
 * Interface for an object which opens connections to the ShopMate API.
 * Use this to acquire a session object.
 */
public interface ShopMateService {

    /**
     * Asynchronously requests to create a shopping list.
     *
     * @param fbToken The user's Facebook token.
     * @param title The title of the list to create.
     * @param inviteUserIds The IDs of additional users to invite.
     * @return The actual shopping list that was created.
     */
    ListenableFuture<CreateShoppingListResult> createListAsync(String fbToken, String title, ImmutableSet<String> inviteUserIds);

    /**
     * Asynchronously requests to delete a shopping list.
     * The user must be the list's creator.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to delete.
     */
    ListenableFuture<Void> deleteListAsync(String fbToken, long listId);

    /**
     * Asynchronously requests to leave a shopping list.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to leave.
     */
    ListenableFuture<Void> leaveListAsync(String fbToken, long listId);

    /**
     * Asynchronously requests to remove a user from a shopping list.
     * The requesting user must be the list's creator.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to remove a user from.
     * @param removeUserId The FBID of the user to remove from the list.
     */
    ListenableFuture<Void> removeUserFromListAsync(String fbToken, long listId, String removeUserId);

    /**
     * Asynchronously gets information about a single shopping list and the items in it.
     * The user must have access to the list or else this will fail with a BAD_REQUEST error.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to get.
     * @return The full information for the shopping list.
     */
    ListenableFuture<ShoppingList> getListAndItemsAsync(String fbToken, long listId);

    /**
     * Asynchronously gets all shopping lists that the user is a member of.
     * The returned lists will not have complete item information (item handles will be empty).
     *
     * @param fbToken The user's Facebook token.
     * @return The shopping lists.
     */
    ListenableFuture<GetAllShoppingListsResult> getAllListsNoItemsAsync(String fbToken);

    /**
     * Asynchronously gets all shopping lists that the user is a member of,
     * alongside full information for all of the items in each list.
     *
     * @param fbToken The user's Facebook token.
     * @return The shopping lists.
     */
    ListenableFuture<GetAllShoppingListsResult> getAllListsAndItemsAsync(String fbToken);

    /**
     * Asynchronously creates an item and adds it to a shopping list.
     * The user must have access to the list or else this will fail with a BAD_REQUEST error.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to add the item to.
     * @param item The item to create. (You can use ShoppingListItemBuilder to make things easier.)
     * @return The actual item that was created.
     */
    ListenableFuture<CreateShoppingListItemResult> createItemAsync(String fbToken, long listId, ShoppingListItem item);

    /**
     * Asynchronously updates an item.
     * The user must have access to the item's list or else this will fail with a BAD_REQUEST error.
     *
     * @param fbToken The user's Facebook token.
     * @param itemId The ID of the item to update.
     * @param update The changes to make to the item. Use ShoppingListItemUpdate.fromDifference() to build this easily.
     * @return The updated item.
     */
    ListenableFuture<ShoppingListItem> updateItemAsync(String fbToken, long itemId, ShoppingListItemUpdate update);

    /**
     * Asynchronously deletes an item.
     * The user must have access to the item's list or else this will fail with a BAD_REQUEST error.
     *
     * @param fbToken The user's Facebook token.
     * @param itemId The ID of the item to delete.
     */
    ListenableFuture<Void> deleteItemAsync(String fbToken, long itemId);

    /**
     * Asynchronously gets information about an item.
     * The user must have access to the item's list or else this will fail with a BAD_REQUEST error.
     *
     * @param fbToken The user's Facebook token.
     * @param itemId The ID of the item to get.
     * @return The item.
     */
    ListenableFuture<ShoppingListItem> getItemAsync(String fbToken, long itemId);

    /**
     * Asynchronously gets all pending invites that the user has sent or received.
     *
     * @param fbToken The user's Facebook token.
     * @return The invites.
     */
    ListenableFuture<GetAllInvitesResult> getAllInvites(String fbToken);

    /**
     * Asynchronously sends an invite for a user to join a list.
     *
     * @param fbToken The user's Facebook token.
     * @param listId The ID of the list to send an invite for.
     * @param receiverUserId The FBID of the user to send the invite to.
     * @return Information about the invite that was sent.
     */
    ListenableFuture<SendInviteResult> sendInviteAsync(String fbToken, long listId, String receiverUserId);

    /**
     * Asynchronously accepts an invite that was sent to a user.
     *
     * @param fbToken The user's Facebook token.
     * @param inviteId The ID of the invite to accept.
     */
    ListenableFuture<Void> acceptInviteAsync(String fbToken, long inviteId);

    /**
     * Asynchronously declines an invite that was sent to a user.
     *
     * @param fbToken The user's Facebook token.
     * @param inviteId The ID of the invite to decline.
     */
    ListenableFuture<Void> declineInviteAsync(String fbToken, long inviteId);

    /**
     * Asynchronously cancels a pending invite that a user sent.
     *
     * @param fbToken The user's Facebook token.
     * @param inviteId The ID of the invite to cancel.
     */
    ListenableFuture<Void> cancelInviteAsync(String fbToken, long inviteId);

    /**
     * Asynchronously registers a Firebase Cloud Messaging (FCM) device token.
     *
     * @param fbToken The user's Facebook token.
     * @param fcmToken The FCM token to register.
     */
    ListenableFuture<Void> registerFcmTokenAsync(String fbToken, String fcmToken);

    /**
     * Asynchronously logs a purchase.
     *
     * @param fbToken The user's Facebook token.
     * @param itemId The ID of the item that was purchased.
     * @param receiverUserId The FBID of the user to request reimbursement from.
     * @param totalPriceCents The total cost of the purchase in cents.
     * @param quantity The number of items purchased.
     * @return The purchase that was made.
     */
    ListenableFuture<ShoppingItemPurchase> makePurchaseAsync(
            String fbToken, long itemId, String receiverUserId, int totalPriceCents, int quantity);

    /**
     * Asynchronously gets information about a purchase.
     *
     * @param fbToken The user's Facebook token.
     * @param purchaseId The ID of the purchase to get.
     * @return The purchase.
     */
    ListenableFuture<ShoppingItemPurchase> getPurchaseAsync(String fbToken, long purchaseId);

    /**
     * Asynchronously gets all purchases which involve a user.
     *
     * @param fbToken The user's Facebook token.
     * @return The list of purchases.
     */
    ListenableFuture<ImmutableList<ShoppingItemPurchase>> getAllPurchasesAsync(String fbToken);

    /**
     * Asynchronously marks a purchase as reimbursed.
     *
     * @param fbToken The user's Facebook token.
     * @param purchaseId The ID of the purchase to complete.
     */
    ListenableFuture<Void> completePurchaseAsync(String fbToken, long purchaseId);
}
