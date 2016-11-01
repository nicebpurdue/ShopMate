package com.shopmate.api.net;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.reflect.TypeToken;
import com.shopmate.api.ShopMateErrorCode;
import com.shopmate.api.ShopMateException;
import com.shopmate.api.ShopMateService;
import com.shopmate.api.model.item.ShoppingListItem;
import com.shopmate.api.model.list.ShoppingList;
import com.shopmate.api.model.result.CreateShoppingListItemResult;
import com.shopmate.api.model.result.CreateShoppingListResult;
import com.shopmate.api.model.result.GetAllShoppingListsResult;
import com.shopmate.api.net.model.list.ShoppingListJson;
import com.shopmate.api.net.model.request.CreateItemRequest;
import com.shopmate.api.net.model.request.CreateListRequest;
import com.shopmate.api.net.model.request.GetAllListsRequest;
import com.shopmate.api.net.model.request.GetListRequest;
import com.shopmate.api.net.model.response.ApiResponse;
import com.shopmate.api.net.model.response.CreateItemResponse;
import com.shopmate.api.net.model.response.ErrorCodes;
import com.shopmate.api.net.model.response.GetAllListsResponse;
import com.shopmate.api.net.model.response.GetItemResponse;
import com.shopmate.api.net.model.response.GetListResponse;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class NetShopMateService implements ShopMateService {
    private static final String BaseUrl = "http://45.55.87.46/api/";

    private static final String CreateListUrl = "/list/create";
    private static final String AllListsUrl = "/list/all";
    private static final String GetListUrl = "/list/%s";

    private static final String CreateItemUrl = "/item/create";
    private static final String GetItemUrl = "/item/%s";

    private static ListeningExecutorService ThreadPool =
            MoreExecutors.listeningDecorator(
                    MoreExecutors.getExitingExecutorService(
                            (ThreadPoolExecutor)Executors.newCachedThreadPool()));

    private final JsonEndpoint endpoint;

    public NetShopMateService() {
        try {
            endpoint = new JsonEndpoint(BaseUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ListenableFuture<CreateShoppingListResult> createListAsync(final String fbToken, final String title) {
        return ThreadPool.submit(new Callable<CreateShoppingListResult>() {
            @Override
            public CreateShoppingListResult call() throws Exception {
                CreateListRequest request = new CreateListRequest(fbToken, title);
                Type responseType = new TypeToken<ApiResponse<ShoppingListJson>>(){}.getType();
                ApiResponse<ShoppingListJson> response = post(CreateListUrl, request, responseType);
                throwIfRequestFailed(response);
                ShoppingListJson result = response.getResult().get();
                return new CreateShoppingListResult(result.getId(), result.toShoppingList());
            }
        });
    }

    @Override
    public ListenableFuture<ShoppingList> getListAndItemsAsync(final String fbToken, final long listId) {
        return ThreadPool.submit(new Callable<ShoppingList>() {
            @Override
            public ShoppingList call() throws Exception {
                GetListRequest request = new GetListRequest(fbToken);
                String url = String.format(GetListUrl, listId);
                Type responseType = new TypeToken<ApiResponse<GetListResponse>>(){}.getType();
                ApiResponse<GetListResponse> response = post(url, request, responseType);
                throwIfRequestFailed(response);
                return response.getResult().get().toShoppingList();
            }
        });
    }

    @Override
    public ListenableFuture<GetAllShoppingListsResult> getAllListsNoItemsAsync(String fbToken) {
        return getAllShoppingListsInternal(fbToken, false);
    }

    @Override
    public ListenableFuture<GetAllShoppingListsResult> getAllListsAndItemsAsync(String fbToken) {
        return getAllShoppingListsInternal(fbToken, true);
    }

    private ListenableFuture<GetAllShoppingListsResult> getAllShoppingListsInternal(final String fbToken, final boolean withItems) {
        return ThreadPool.submit(new Callable<GetAllShoppingListsResult>() {
            @Override
            public GetAllShoppingListsResult call() throws Exception {
                GetAllListsRequest request = new GetAllListsRequest(fbToken, withItems);
                Type responseType = new TypeToken<ApiResponse<GetAllListsResponse>>(){}.getType();
                ApiResponse<GetAllListsResponse> response = post(AllListsUrl, request, responseType);
                throwIfRequestFailed(response);
                return response.getResult().get().toResult();
            }
        });
    }

    @Override
    public ListenableFuture<CreateShoppingListItemResult> createItemAsync(final String fbToken, final long listId, final ShoppingListItem item) {
        return ThreadPool.submit(new Callable<CreateShoppingListItemResult>() {
            @Override
            public CreateShoppingListItemResult call() throws Exception {
                CreateItemRequest request = new CreateItemRequest(fbToken, listId, item);
                Type responseType = new TypeToken<ApiResponse<CreateItemResponse>>(){}.getType();
                ApiResponse<CreateItemResponse> response = post(CreateItemUrl, request, responseType);
                throwIfRequestFailed(response);
                return response.getResult().get().toResult();
            }
        });
    }

    @Override
    public ListenableFuture<ShoppingListItem> getItemAsync(final String fbToken, final long itemId) {
        return ThreadPool.submit(new Callable<ShoppingListItem>() {
            @Override
            public ShoppingListItem call() throws Exception {
                GetListRequest request = new GetListRequest(fbToken);
                String url = String.format(GetItemUrl, itemId);
                Type responseType = new TypeToken<ApiResponse<GetItemResponse>>(){}.getType();
                ApiResponse<GetItemResponse> response = post(url, request, responseType);
                throwIfRequestFailed(response);
                return response.getResult().get().toItem();
            }
        });
    }

    private <TRequest, TResponse> TResponse post(String url, TRequest request, Type responseType) throws ShopMateException {
        try {
            return endpoint.post(url, request, responseType);
        } catch (IOException e) {
            throw new ShopMateException(ShopMateErrorCode.CONNECTION_ERROR, e);
        }
    }

    private <TResult> void throwIfRequestFailed(ApiResponse<TResult> response) throws ShopMateException {
        if (!response.getResult().isPresent()) {
            ShopMateErrorCode code = ErrorCodes.getErrorCode(response.getError().orNull());
            String message = ErrorCodes.getErrorMessage(code);
            throw new ShopMateException(code, message);
        }
    }
}