package com.shopmate.shopmate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.widget.LoginButton;
import com.facebook.share.model.AppInviteContent;
import com.facebook.share.widget.AppInviteDialog;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.appindexing.Thing;
import com.google.android.gms.common.api.BooleanResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wallet.Cart;
import com.google.android.gms.wallet.FullWalletRequest;
import com.google.android.gms.wallet.IsReadyToPayRequest;
import com.google.android.gms.wallet.LineItem;
import com.google.android.gms.wallet.MaskedWallet;
import com.google.android.gms.wallet.MaskedWalletRequest;
import com.google.android.gms.wallet.PaymentMethodTokenizationParameters;
import com.google.android.gms.wallet.PaymentMethodTokenizationType;
import com.google.android.gms.wallet.Wallet;
import com.google.android.gms.wallet.WalletConstants;
import com.google.android.gms.wallet.fragment.SupportWalletFragment;
import com.google.android.gms.wallet.fragment.WalletFragmentInitParams;
import com.google.android.gms.wallet.fragment.WalletFragmentMode;
import com.google.android.gms.wallet.fragment.WalletFragmentOptions;
import com.google.android.gms.wallet.fragment.WalletFragmentStyle;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.shopmate.api.ShopMateService;
import com.shopmate.api.ShopMateServiceProvider;
import com.shopmate.api.model.list.ShoppingList;
import com.shopmate.api.model.result.CreateShoppingListResult;
import com.shopmate.api.model.result.GetAllShoppingListsResult;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static LoginButton loginButton;
    static CallbackManager callbackManager;
    static int i = 1;

    private UpdateListener updateListener;
    private ShoppingListAdapter shoppingLists;
    private final ShopMateService service = ShopMateServiceProvider.get();
    private GoogleApiClient payClient;

    final int REQUEST_CODE_MASKED_WALLET = 5;
    final int REQUEST_CODE_FULL_WALLET = 7;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    public Action getIndexApiAction() {
        Thing object = new Thing.Builder()
                .setName("Main Page") // TODO: Define a title for the content shown.
                // TODO: Make sure this auto-generated URL is correct.
                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
                .build();
        return new Action.Builder(Action.TYPE_VIEW)
                .setObject(object)
                .setActionStatus(Action.STATUS_TYPE_COMPLETED)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        AppIndex.AppIndexApi.start(client, getIndexApiAction());
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        AppIndex.AppIndexApi.end(client, getIndexApiAction());
        client.disconnect();
    }

    private class ShoppingListEntry {
        private final long id;
        private final ShoppingList list;

        public ShoppingListEntry(long id, ShoppingList list) {
            this.id = id;
            this.list = list;
        }

        public long getId() {
            return id;
        }

        public ShoppingList getList() {
            return list;
        }
    }

    private class ShoppingListAdapter extends ArrayAdapter<ShoppingListEntry> {
        private List<ShoppingListEntry> items;
        private Context context;
        private int layout;

        ShoppingListAdapter(Context context, int resourceId, List<ShoppingListEntry> items) {
            super(context, resourceId, items);
            this.items = items;
            this.context = context;
            this.layout = resourceId;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = View.inflate(context, layout, null);
            } else {
                view = convertView;
            }
            TextView title = (TextView) view.findViewById(R.id.label);
            title.setText(items.get(position).getList().getTitle());

            return view;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FacebookSdk.sdkInitialize(getApplicationContext());

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navView = (NavigationView) findViewById(R.id.nav_view);
        final View navHeader = navView.getHeaderView(0);

        if (isLoggedIn()) {
            loginButton = LoginActivity.getLoginButton();
            callbackManager = LoginActivity.getCallbackManager();

            //Gets the name of the user
            new GraphRequest(AccessToken.getCurrentAccessToken(), "/me?fields=id,name,picture.type(large)", null,
                    HttpMethod.GET, new GraphRequest.Callback() {
                public void onCompleted(GraphResponse response) {
                    //handle the response
                    final JSONObject jsonObject = response.getJSONObject();
                    String name = "";
                    try {
                        TextView user_name = (TextView) navHeader.findViewById(R.id.usertextView);
                        ImageView user_picture = (ImageView) navHeader.findViewById(R.id.userimageView);

                        name = jsonObject.getString("name");
                        String firstName = name.substring(0, name.indexOf(" "));
                        String lastName = name.substring(name.indexOf(" ") + 1);
                        user_name.setText(name);
                        final JSONObject picObj = jsonObject.getJSONObject("picture");
                        final JSONObject dataObj = picObj.getJSONObject("data");
                        String url = dataObj.getString("url");
                        System.out.println("URL: " + url);
                        Picasso.with(getApplicationContext()).load(url).into(user_picture);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }).executeAsync();
        }

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //make a dummy list for the home screen
        final ListView listview = (ListView) findViewById(R.id.userlistView);
        //using arraylists to store data just for the sake of having data
        //TODO: Create a custom Adapter class to take in HashMaps instead to make this more efficient

        shoppingLists = new ShoppingListAdapter(this, R.layout.rowlayout, new ArrayList<ShoppingListEntry>());
        listview.setAdapter(shoppingLists);

        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(view.getContext(), ShoppingListActivity.class);
                Bundle extras = new Bundle();
                ShoppingListEntry entry = (ShoppingListEntry) parent.getItemAtPosition(position);
                extras.putString("title", entry.getList().getTitle());
                extras.putString("listId", Long.toString(entry.getId()));
                extras.putString("listOwner", entry.getList().getCreatorId());
                i.putExtras(extras);
                startActivity(i);
            }
        });

        UpdateShoppingLists();

        ((Button) findViewById(R.id.addNewListButton)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String fbToken = AccessToken.getCurrentAccessToken().getToken();
                final ImmutableSet<String> invites = ImmutableSet.of();


                final EditText txtUrl = new EditText(MainActivity.this);

                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("New List Title")
                        .setView(txtUrl)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                String listTitle = txtUrl.getText().toString();
                                if (listTitle.length() == 0) {
                                    return;
                                }
                                Futures.addCallback(service.createListAsync(fbToken, listTitle, invites), new FutureCallback<CreateShoppingListResult>() {
                                    @Override
                                    public void onSuccess(CreateShoppingListResult result) {
                                        final ShoppingListEntry tmp = new ShoppingListEntry(result.getId(), result.getList());
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (findShoppingList(tmp.getId()) < 0) {
                                                    shoppingLists.insert(tmp, 0);
                                                }
                                            }
                                        });
                                        Snackbar.make(listview, "success", Snackbar.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        Snackbar.make(listview, "defeat", Snackbar.LENGTH_LONG).show();
                                    }
                                });
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                return;
                            }
                        })
                        .show();


            }
        });

        // Always register the FCM token with the server
        InstanceIdService.registerFcmToken();

        updateListener = new UpdateListener(this, new UpdateHandler() {
            @Override
            public void onListShared(final long listId) {
                if (findShoppingList(listId) >= 0) {
                    return;
                }
                String fbToken = AccessToken.getCurrentAccessToken().getToken();
                Futures.addCallback(service.getListAndItemsAsync(fbToken, listId), new FutureCallback<ShoppingList>() {
                    @Override
                    public void onSuccess(ShoppingList result) {
                        final ShoppingListEntry entry = new ShoppingListEntry(listId, result);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (findShoppingList(listId) < 0) {
                                    shoppingLists.insert(entry, 0);
                                }
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                    }
                });
            }

            @Override
            public void onListDeleted(long listId) {
                int index = findShoppingList(listId);
                if (index >= 0) {
                    shoppingLists.remove(shoppingLists.getItem(index));
                }
            }

            @Override
            public void onListMemberLeft(long listId, String userId) {
                if (userId == AccessToken.getCurrentAccessToken().getUserId()) {
                    onListDeleted(listId);
                }
            }
        });
        updateListener.register();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
//        payClient = new GoogleApiClient.Builder(this)
//                .addApi(Wallet.API,
//                        new Wallet.WalletOptions.Builder()
//                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
//                                .build())
//                .build();
//        Toast.makeText(this, "starting", Toast.LENGTH_SHORT).show();
//        payClient.connect();
//        Wallet.Payments.isReadyToPay(payClient, IsReadyToPayRequest.newBuilder().build())
//                .setResultCallback(new ResultCallback<BooleanResult>() {
//            @Override
//            public void onResult(@NonNull BooleanResult booleanResult) {
//                if (booleanResult.getStatus().isSuccess()) {
//                    if (booleanResult.getValue()) {
//                        Toast.makeText(MainActivity.this, "success", Toast.LENGTH_SHORT).show();
//
//                        PaymentMethodTokenizationParameters parameters = PaymentMethodTokenizationParameters.newBuilder()
//                                .setPaymentMethodTokenizationType(PaymentMethodTokenizationType.NETWORK_TOKEN)
//                                .addParameter("publicKey", "BO39Rh43UGXMQy5PAWWe7UGWd2a9YRjNLPEEVe+zWIbdIgALcDcnYCuHbmrrzl7h8FZjl6RCzoi5/cDrqXNRVSo=")
//                                .build();
//                        MaskedWalletRequest request = MaskedWalletRequest.newBuilder()
//                                .setCurrencyCode("USD")
//                                .setEstimatedTotalPrice("15.00")
//                                .setPaymentMethodTokenizationParameters(parameters)
//                                .build();
//                        WalletFragmentStyle walletFragmentStyle = new WalletFragmentStyle()
//                                .setBuyButtonText(WalletFragmentStyle.BuyButtonText.BUY_WITH)
//                                .setBuyButtonAppearance(WalletFragmentStyle.BuyButtonAppearance.ANDROID_PAY_DARK)
//                                .setBuyButtonWidth(WalletFragmentStyle.Dimension.MATCH_PARENT);
//
//                        WalletFragmentOptions walletFragmentOptions = WalletFragmentOptions.newBuilder()
//                                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
//                                .setFragmentStyle(walletFragmentStyle)
//                                .setTheme(WalletConstants.THEME_LIGHT)
//                                .setMode(WalletFragmentMode.BUY_BUTTON)
//                                .build();
//
//                        SupportWalletFragment mWalletFragment = SupportWalletFragment.newInstance(walletFragmentOptions);
//
//                        WalletFragmentInitParams.Builder startParamsBuilder =
//                                WalletFragmentInitParams.newBuilder()
//                                        .setMaskedWalletRequest(request)
//                                        .setMaskedWalletRequestCode(REQUEST_CODE_MASKED_WALLET);
//                        mWalletFragment.initialize(startParamsBuilder.build());
//
//                        // add Wallet fragment to the UI
//                        getSupportFragmentManager().beginTransaction()
//                                .replace(R.id.dynamic_wallet_button_fragment, mWalletFragment)
//                                .commit();
//
//                        // show pay button
//                    } else {
//                        Toast.makeText(MainActivity.this, "failure", Toast.LENGTH_SHORT).show();
//                        // show not pay button
//                    }
//                } else {
//                    Toast.makeText(MainActivity.this, "wtf", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_MASKED_WALLET:
                if (resultCode == RESULT_OK) {
                    MaskedWallet maskedWallet = data.getParcelableExtra(WalletConstants.EXTRA_MASKED_WALLET);
                    Log.d("things", maskedWallet.getEmail());
                    String googleTransactionId = maskedWallet.getGoogleTransactionId();

                    TextView txt = new TextView(this);
                    txt.setText("You are approving a transaction of $15.00");
                    final FullWalletRequest request = FullWalletRequest.newBuilder()
                            .setGoogleTransactionId(googleTransactionId)
                            .setCart(Cart.newBuilder()
                                    .setCurrencyCode("USD")
                                    .setTotalPrice("15.00")
                                    .addLineItem(LineItem.newBuilder()
                                            .setDescription("a thingy")
                                            .setQuantity("1")
                                            .setUnitPrice("15.00")
                                            .setTotalPrice("15.00")
                                            .setCurrencyCode("USD")
                                            .build()
                                    )
                                    .build())
                            .build();
                    new AlertDialog.Builder(this)
                            .setTitle("Are you sure?")
                            .setView(txt)
                            .setPositiveButton("Accept", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Wallet.Payments.loadFullWallet(payClient, request, REQUEST_CODE_FULL_WALLET);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    return;
                                }
                            })
                            .show();
                }
                break;
            case WalletConstants.RESULT_ERROR:
                Log.d("things", Integer.toString(data.getIntExtra(WalletConstants.EXTRA_ERROR_CODE, -1)));
                break;
            case REQUEST_CODE_FULL_WALLET:
                if (resultCode == RESULT_OK) {
                    Log.d("things", "it worked");
                } else {
                    Log.d("things", "it didn't work");
                }
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateListener.unregister();
    }

    //    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//        View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
//        TextView textView = (TextView) rowView.findViewById(R.id.label);
//        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
//        textView.setText(values[position]);
//        // change the icon for Windows and iPhone
//        String s = values[position];
//        if (s.startsWith("iPhone")) {
//            imageView.setImageResource(R.drawable.no);
//        } else {
//            imageView.setImageResource(R.drawable.ok);
//        }
//
//        return rowView;
//    }

    public boolean isLoggedIn() {
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        return accessToken != null;
    }

    @Override
    public void onResume() {
        super.onResume();
        UpdateShoppingLists();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_logout) {
            // Handle the logout action
            Intent init = new Intent(MainActivity.this, LoginActivity.class);
            init.putExtra("FromNavMenu", true);
            startActivity(init);

        } else if (id == R.id.nav_shared) {

        } else if (id == R.id.nav_personal) {

        } else if (id == R.id.nav_list_invites) {
            startActivity(new Intent(MainActivity.this, InviteRequestsActivity.class));
        } else if (id == R.id.nav_list_req_history) {
            startActivity(new Intent(MainActivity.this, RequestHistoryActivity.class));
        } else if (id == R.id.nav_share) {
            String appLinkUrl, previewImageUrl;

            appLinkUrl = "https://fb.me/1210406722382112";
            previewImageUrl = "https://i.imgur.com/UWuC035.png";

            if (AppInviteDialog.canShow()) {
                AppInviteContent content = new AppInviteContent.Builder()
                        .setApplinkUrl(appLinkUrl)
                        .setPreviewImageUrl(previewImageUrl)
                        .build();
                AppInviteDialog.show(this, content);
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void UpdateShoppingLists() {
        final ListView listview = (ListView) findViewById(R.id.userlistView);
        String fbToken = AccessToken.getCurrentAccessToken().getToken();

        Futures.addCallback(service.getAllListsAndItemsAsync(fbToken), new FutureCallback<GetAllShoppingListsResult>() {
            @Override
            public void onSuccess(GetAllShoppingListsResult result) {
                final List<ShoppingListEntry> tmp = new ArrayList<>();
                for (Map.Entry<Long, ShoppingList> i : result.getLists().entrySet()) {
                    tmp.add(new ShoppingListEntry(i.getKey(), i.getValue()));
                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        shoppingLists.clear();
                        shoppingLists.addAll(tmp);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Snackbar.make(listview, "defeat", Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private int findShoppingList(long id) {
        for (int i = 0; i < shoppingLists.getCount(); i++) {
            ShoppingListEntry entry = shoppingLists.getItem(i);
            if (entry.getId() == id) {
                return i;
            }
        }
        return -1;
    }
}
