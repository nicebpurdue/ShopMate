package com.shopmate.shopmate;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.shopmate.api.ShopMateService;
import com.shopmate.api.model.item.ShoppingListItemUpdate;
import com.shopmate.api.model.purchase.ShoppingItemPurchase;
import com.shopmate.api.net.NetShopMateService;
import com.squareup.picasso.Picasso;
import com.facebook.AccessToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.shopmate.api.ShopMateServiceProvider;
import com.shopmate.api.model.item.ShoppingListItem;
import com.shopmate.api.model.item.ShoppingListItemBuilder;
import com.shopmate.api.model.item.ShoppingListItemHandle;
import com.shopmate.api.model.item.ShoppingListItemPriority;
import com.shopmate.api.model.list.ShoppingList;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


public class ShoppingListActivity extends AppCompatActivity {

    NetShopMateService netShopMateService = new NetShopMateService();
    static final int ADD_ITEM_REQUEST = 1;
    private static ShoppingListItemAdapter sla;
    private Comparator<ShoppingListItemHandle> comparator = new PrioComparator();
    private UpdateListener updateListener;
    private long listId;
    private String listOwner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_list);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Bundle extras = getIntent().getExtras();
        final String title = extras.getString("title");
        listId = Long.parseLong(extras.getString("listId"));
        listOwner = extras.getString("listOwner");

        toolbar.setTitle(title);
        setSupportActionBar(toolbar);

        // These are just some mock items to add to the list.
        // TODO retrieve these items from the database
        final List<ShoppingListItemHandle> items = new ArrayList<ShoppingListItemHandle>();

        // Creates an adapter which is used maintain and render a list of items
        final ListView shoppingList = (ListView) findViewById(R.id.shoppingList);
        ShoppingListItemAdapter shoppingListItemAdapter = new ShoppingListItemAdapter(this, R.layout.shopping_list_item, items);
        sla = shoppingListItemAdapter;
        assert shoppingList != null;
        shoppingList.setAdapter(shoppingListItemAdapter);

        Futures.addCallback(ShopMateServiceProvider.get().getListAndItemsAsync(AccessToken.getCurrentAccessToken().getToken(), listId), new FutureCallback<ShoppingList>() {
            @Override
            public void onSuccess(final ShoppingList result) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sla.addAll(result.getItems());
                        sla.sort(comparator);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                Snackbar.make(shoppingList, "defeat", Snackbar.LENGTH_LONG).show();
            }
        });

        // Adds a new item to this shopping list when the button is pressed
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        assert fab != null;
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean buy = false;
                HashMap<Long, ShoppingListItemAdapter.State> stateMap = sla.getStateMap();
                Set<Map.Entry<Long, ShoppingListItemAdapter.State>> entries = stateMap.entrySet();
                for (Map.Entry<Long, ShoppingListItemAdapter.State> s : entries) {
                    if (s.getValue().quantity > 0) {
                        buy = true;
                        break;
                    }
                }
                if (buy) { // buy some items
                    for (Map.Entry<Long, ShoppingListItemAdapter.State> s: entries) {
                        if (s.getValue().quantity > 0) { //hold on to your butts, we're buying snakes in a plane!
                            final ShoppingListItem buyItem = s.getValue().item;
                            final Long buyId = s.getKey();
                            final int buyQuant = s.getValue().quantity;
                            Futures.addCallback(ShopMateServiceProvider.get().makePurchaseAsync(
                                    AccessToken.getCurrentAccessToken().getToken(),
                                    s.getKey(),
                                    listOwner,
                                    s.getValue().item.getMaxPriceCents().or(0) * s.getValue().quantity,
                                    s.getValue().quantity
                            ), new FutureCallback<ShoppingItemPurchase>() {
                                @Override
                                public void onSuccess(ShoppingItemPurchase result) {
                                    ShoppingListItemUpdate up = new ShoppingListItemUpdate(
                                            Optional.<String>absent(),
                                            Optional.<String>absent(),
                                            Optional.<Optional<String>>absent(),
                                            Optional.<Optional<Integer>>absent(),
                                            Optional.of(buyItem.getQuantity() - buyQuant),
                                            0,
                                            Optional.<ShoppingListItemPriority>absent());
                                    Futures.addCallback(ShopMateServiceProvider.get().updateItemAsync(
                                            AccessToken.getCurrentAccessToken().getToken(),
                                            buyId,
                                            up
                                    ), new FutureCallback<ShoppingListItem>() {
                                        @Override
                                        public void onSuccess(ShoppingListItem result) {
                                            return;
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            return;
                                        }
                                    });
                                     // TODO remove the item from the list if it's completely bought, I think
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ShoppingListActivity.this, "item bought", Toast.LENGTH_LONG).show();
                                        }
                                    });

                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    //TODO something here?
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(ShoppingListActivity.this, "something went wrong", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            });
                        }
                    }
                } else { // add shopping list item
                    Intent i = new Intent(view.getContext(), AddItemActivity.class);
                    Bundle extras = new Bundle();
                    extras.putString("title", title);
                    extras.putString("listId", Long.toString(listId));
                    i.putExtras(extras);
                    startActivityForResult(i, ADD_ITEM_REQUEST);
                }
            }
        });

        updateListener = new UpdateListener(this, new UpdateHandler() {
            @Override
            public void onItemAdded(long itemListId, final long itemId) {
                if (itemListId != listId || findItem(itemId) >= 0) {
                    return;
                }
                String fbToken = AccessToken.getCurrentAccessToken().getToken();
                ShopMateService service = ShopMateServiceProvider.get();
                Futures.addCallback(service.getItemAsync(fbToken, itemId), new FutureCallback<ShoppingListItem>() {
                    @Override
                    public void onSuccess(final ShoppingListItem result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (findItem(itemId) >= 0) {
                                    return;
                                }
                                sla.add(new ShoppingListItemHandle(itemId, Optional.of(result)));
                                sla.sort(comparator);
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
            public void onItemUpdated(final long itemId) {
                if (findItem(itemId) < 0) {
                    return;
                }
                String fbToken = AccessToken.getCurrentAccessToken().getToken();
                ShopMateService service = ShopMateServiceProvider.get();
                Futures.addCallback(service.getItemAsync(fbToken, itemId), new FutureCallback<ShoppingListItem>() {
                    @Override
                    public void onSuccess(final ShoppingListItem result) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int index = findItem(itemId);
                                if (index < 0) {
                                    return;
                                }
                                sla.remove(sla.getItem(index));
                                sla.add(new ShoppingListItemHandle(itemId, Optional.of(result)));
                                sla.sort(comparator);
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
            public void onItemDeleted(long itemId) {
                int index = findItem(itemId);
                if (index >= 0) {
                    sla.remove(sla.getItem(index));
                }
            }

            @Override
            public void onListDeleted(long deletedListId) {
                if (deletedListId == listId) {
                    // TODO: Display a message telling the user that the list was deleted?
                    finish();
                }
            }

            @Override
            public void onListMemberLeft(long leftListId, String userId) {
                if (leftListId == listId && userId == AccessToken.getCurrentAccessToken().getUserId()) {
                    // TODO: Display a message telling the user that they were kicked?
                    finish();
                }
            }
        });
        updateListener.register();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        updateListener.unregister();
    }

    private int findItem(long itemId) {
        for (int i = 0; i < sla.getCount(); i++) {
            if (sla.getItem(i).getId() == itemId) {
                return i;
            }
        }
        return -1;
    }

    //Converts a string priority to the ShoppingListItemPriority enum
    private ShoppingListItemPriority convertPriority(String priority){
        switch(priority){
            case "Low":
                return ShoppingListItemPriority.LOW;
            case "Medium":
                return ShoppingListItemPriority.NORMAL;
            case "High":
                return ShoppingListItemPriority.HIGH;
            default:
                return ShoppingListItemPriority.LOW;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == ADD_ITEM_REQUEST) {
                final Intent d = data;
                double price = Double.parseDouble(d.getStringExtra("item_price"));
                price *= 100;
                final ShoppingListItemBuilder bld = new ShoppingListItemBuilder(null)
                        .name(d.getStringExtra("item_name"))
                        .priority(convertPriority(d.getStringExtra("item_prio")))
                        .imageUrl(d.hasExtra("item_img") ? d.getStringExtra("item_img") : "")
                        .priority(convertPriority(d.getStringExtra("item_prio")))
                        .quantity(Integer.parseInt(d.getStringExtra("item_quan")))
                        .maxPriceCents(((int) price));

                final long id = Long.parseLong(d.getStringExtra("item_id"));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (findItem(id) >= 0) {
                            return;
                        }
                        sla.add(new ShoppingListItemHandle(id, Optional.of(bld.build())));
                        sla.sort(comparator);
                    }
                });
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sorting, menu);
        getMenuInflater().inflate(R.menu.share_button_menu, menu);
        return true;
    }

    private class AlphaComparator implements Comparator<ShoppingListItemHandle> {

        @Override
        public int compare(ShoppingListItemHandle lhs, ShoppingListItemHandle rhs) {
            return lhs.getItem().get().getName().compareToIgnoreCase(rhs.getItem().get().getName());
        }
    }

    private class PrioComparator implements Comparator<ShoppingListItemHandle> {

        @Override
        public int compare(ShoppingListItemHandle lhs, ShoppingListItemHandle rhs) {
            int comparison = rhs.getItem().get().getPriority().compareTo(lhs.getItem().get().getPriority()); // given current enum, this orders HIGH to LOW
            if (comparison != 0) {
                return comparison;
            }
            // Compare by name if two items have the same priority
            return lhs.getItem().get().getName().compareToIgnoreCase(rhs.getItem().get().getName());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.sort_alpha) {
            comparator = new AlphaComparator();
            sla.sort(comparator);
            return true;
        } else if (id == R.id.sort_prio) {
            comparator = new PrioComparator();
            sla.sort(comparator);
            return true;
        } else if (id == R.id.leave_list) {
            netShopMateService.leaveListAsync(AccessToken.getCurrentAccessToken().getToken(), listId);
            finish();
        } else if (id == R.id.share_button){
            //Open up an activity to select the friend who you want to share a list with
            Intent intent = new Intent(ShoppingListActivity.this, SharingListsActivity.class);
            intent.putExtra("listId", listId);
            startActivity(intent);
        } else if (id == R.id.show_members) {
            Intent intent = new Intent(this, ListMembersActivity.class);
            intent.putExtra("listId", listId);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
    }

    private class ShoppingListItemAdapter extends ArrayAdapter<ShoppingListItemHandle> {
        private List<ShoppingListItemHandle> items;
        private Context context;
        private int layout;
        private HashMap<Long, State> stateMap;

        public HashMap<Long, State> getStateMap() {
            return stateMap;
        }

        ShoppingListItemAdapter(Context context, int resourceId, List<ShoppingListItemHandle> items) {
            super(context, resourceId, items);
            this.items = items;
            this.context = context;
            this.layout = resourceId;
            this.stateMap = new HashMap<Long, State>();
            for (ShoppingListItemHandle i : items) {
                stateMap.put(i.getId(), new State(0, i.getItem().get()));
            }
        }

        @Override
        public void add(ShoppingListItemHandle object) {
            super.add(object);
            stateMap.put(object.getId(), new State(0, object.getItem().get()));
        }

        @Override
        public void addAll(Collection<? extends ShoppingListItemHandle> collection) {
            super.addAll(collection);
            for (ShoppingListItemHandle i : collection) {
                stateMap.put(i.getId(), new State(0, i.getItem().get()));
            }
        }

        class State {
            public int quantity;
            public ShoppingListItem item;
            public State(int quantity, ShoppingListItem item) {
                this.quantity = quantity;
                this.item = item;
            }
        }

        class ViewHolder {
            public CheckBox checkBox;
            public ImageView imageView;
            public TextView listItemQuantity;
            public TextView listItemPrice;
        }

        @NonNull
        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(context, layout, null);
                holder = new ViewHolder();
                holder.checkBox = (CheckBox) convertView.findViewById(R.id.itemCheckBox);
                holder.checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final CheckBox chk = (CheckBox) v;
                        final Long id = (Long) v.getTag();
                        final ShoppingListItem item = stateMap.get(id).item;
                        if (item.getQuantity() - item.getQuantityPurchased() > 1) { // need to open dialog
                            final EditText txt = new EditText(ShoppingListActivity.this);
                            txt.setInputType(InputType.TYPE_CLASS_NUMBER);
                            if (stateMap.get(id).quantity > 0) { // already has a value
                                txt.setText(Integer.toString(stateMap.get(id).quantity));
                            }
                            new AlertDialog.Builder(ShoppingListActivity.this)
                                    .setTitle("How many?")
                                    .setView(txt)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String q_str = txt.getText().toString();
                                            if (q_str.length() == 0 || Integer.parseInt(q_str) <= 0) { // unselecting
                                                chk.setChecked(false);
                                                stateMap.get(id).quantity = 0;
                                                Log.d("things", id.toString() + ": now has quantity " + Integer.toString(stateMap.get(id).quantity));
                                            } else { // changing selection number
                                                int want = Integer.parseInt(q_str);
                                                int need = stateMap.get(id).item.getQuantity() - stateMap.get(id).item.getQuantityPurchased();
                                                chk.setChecked(true);
                                                if (want > need) { // want more than we need
                                                    stateMap.get(id).quantity = need;
                                                } else { // we can want this much
                                                    stateMap.get(id).quantity = want;
                                                }
                                                Log.d("things", id.toString() + ": now has quantity " + Integer.toString(stateMap.get(id).quantity));
                                            }
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            chk.setChecked(!chk.isChecked()); // reverse the checking motion
                                            Log.d("things", id.toString() + ": now has quantity " + Integer.toString(stateMap.get(id).quantity));
                                        }
                                    })
                                    .show();
                        } else {
                            if (chk.isChecked()) { // now checked
                                stateMap.get(id).quantity = 1;
                                Log.d("things", id.toString() + ": now has quantity " + Integer.toString(stateMap.get(id).quantity));
                            } else { // now unchecked
                                stateMap.get(id).quantity = 0;
                                Log.d("things", id.toString() + ": now has quantity " + Integer.toString(stateMap.get(id).quantity));
                            }
                        }
                    }
                });

                holder.imageView = (ImageView) convertView.findViewById(R.id.itemImageView);
                holder.listItemQuantity = (TextView) convertView.findViewById(R.id.listItemQuantity);
                holder.listItemPrice = (TextView) convertView.findViewById(R.id.listItemPrice);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.checkBox.setTag(items.get(position).getId()); // so that the checkbox can check the right entry in the StateMap
            holder.checkBox.setChecked(stateMap.get(items.get(position).getId()).quantity > 0);

            String itemName = items.get(position).getItem().get().getName();

            holder.checkBox.setText(itemName);

            //Puts the image in for each item
            String imageURL = "http://1030news.com/wp-content/themes/fearless/images/missing-image-640x360.png";
            if(items.get(position).getItem().get().getImageUrl().isPresent() && items.get(position).getItem().get().getImageUrl().get() != "") {
                imageURL = items.get(position).getItem().get().getImageUrl().get();
            }
            if(imageURL.contains("http")) {
                //web location
                Picasso.with(getContext())
                        .load(imageURL)
                        .resize(150, 150)
                        .into(holder.imageView);
            }
            else {
                //phone location
                Picasso.with(getContext())
                        .load(new File(imageURL))
                        .resize(150, 150)
                        .into(holder.imageView);
            }

            //Puts the quantity in for each item
            holder.listItemQuantity.setText("Quantity: " + Integer.toString(items.get(position).getItem().get().getQuantity()));

            //Puts the price in for each item

            double price = ((double) items.get(position).getItem().get().getMaxPriceCents().or(0) / 100);
            holder.listItemPrice.setText("Price: $" + Double.toString(price));

            return convertView;
        }

        @Override
        public void clear() {
            super.clear();
            items.clear();
        }
    }
}
