package com.kapodamy.twilighttweaker;

import android.app.Activity;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TwiAppPick extends AppCompatActivity {

    protected ListView listview = null;
    protected Activity context;
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("listview.state", listview.onSaveInstanceState());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Parcelable listViewState = savedInstanceState.getParcelable("listview.state");
        listview.onRestoreInstanceState(listViewState);

        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar bar = getSupportActionBar();
        bar.setDisplayHomeAsUpEnabled(true);
        bar.setTitle(R.string.s_lista);

        final Application context = ((AppCompatActivity) this).getApplication();

        setContentView(R.layout.activity_twi_app_pick);

        Set<String> set = getSharedPreferences(getString(R.string.prefs_store), MODE_PRIVATE).getStringSet("lista", null);
        final HashSet<String> lista_admision = set == null ? new HashSet<String>() : new HashSet<String>(set.size());

        if (set != null) {
            for (String str : set) {
                lista_admision.add(str);
            }
        }

        final ArrayList<Apk> entries = new ArrayList<Apk>(lista_admision.size());

        for (String entry : lista_admision) {
            try {
                entries.add(new Apk(entry));
            } catch (PackageManager.NameNotFoundException err) {
            }
        }

        final ListAdapter adapter = new ListAdapter(this, R.layout.app_item, entries);
        adapter.onButtonClicked = new Callback<Apk>() {
            @Override
            public void callback(Apk result) {
                lista_admision.remove(result.pkg);
                setAppList(lista_admision);
            }
        };

        if (listview == null) {
            listview = (ListView) findViewById(R.id.lista_apk);
            listview.setEmptyView(findViewById(android.R.id.empty));
            listview.setAdapter(adapter);
            setListViewHeightBasedOnChildren(listview);
        }

        FloatingActionButton agregar = (FloatingActionButton) findViewById(R.id.agregar);
        agregar.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                showDialog(new Callback<Apk[]>() {
                    @Override
                    public void callback(Apk[] apks) {
                        if (apks == null) {
                            Toast.makeText(context, R.string.s_elegir_app_ninguna, Toast.LENGTH_LONG).show();
                            return;
                        }
                        for (Apk apk : apks) {
                            adapter.add(apk);
                            lista_admision.add(apk.pkg);
                        }
                        setAppList(lista_admision);

                    }
                }, (ArrayList<Apk>) entries.clone());
            }
        });
    }

    void setAppList(Set<String> list) {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.prefs_store), MODE_WORLD_READABLE);
        prefs.edit().putStringSet("lista", list).apply();
    }

    public void showDialog(final Callback<Apk[]> fn, final List<Apk> exclude) {
        final View alertLayout = getLayoutInflater().inflate(R.layout.app_choose, null);
        final ListView list = (ListView) alertLayout.findViewById(R.id.app_instaladas);
        final Context context = this;
        final AlertDialog.Builder alert = new AlertDialog.Builder(context);
        final ProgressDialog progressDialog = new ProgressDialog(context);

        TaskListener listener = new TaskListener<Object, ListAdapter>() {
            public void onTaskStarted() {
                progressDialog.setCancelable(false);
                progressDialog.setMessage(getString(R.string.s_cargando_app));
                progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
                progressDialog.show();
            }

            public void onTaskFinished(final ListAdapter adapter) {
                alert.setNegativeButton(R.string.s_cancelar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fn.callback(null);
                    }
                });

                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        fn.callback(adapter.getCheckedValues());
                        adapter.dispose();
                        dialog.dismiss();
                    }
                });

                alert.setOnKeyListener(new DialogInterface.OnKeyListener() {
                    @Override
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        if (i == KeyEvent.KEYCODE_BACK) {
                            adapter.dispose();
                            dialogInterface.dismiss();
                        }
                        return true;
                    }
                });

                alert.setTitle(R.string.s_lista_selec);
                alert.setView(alertLayout);
                alert.setCancelable(false);

                AlertDialog dialog = alert.create();
                progressDialog.dismiss();
                dialog.show();
            }

            public ListAdapter doTask(Object... params) {
                List<PackageInfo> packs = getPackageManager().getInstalledPackages(0);
                ArrayList entries = new ArrayList<Apk>(packs.size());

                for (PackageInfo info : packs) {
                    boolean add = !info.packageName.equals(getPackageName());
                    for (int i = 0; i < exclude.size(); i++) {
                        if (exclude.get(i).pkg.equals(info.packageName)) {
                            add = false;
                            exclude.remove(i);
                            break;
                        }
                    }
                    if (add) {
                        try {
                            entries.add(new Apk(info.packageName));
                        } catch (PackageManager.NameNotFoundException err) {
                        }
                    }
                }

                ListAdapter adapter = new ListAdapter(context, R.layout.app_item, entries);
                list.setAdapter(adapter);
                return adapter;
            }
        };

        BackgroundWoker worker = new BackgroundWoker<Object, Object, ArrayList<Apk>>(listener);
        worker.execute();
    }

    public static void setListViewHeightBasedOnChildren(ListView listView) {
        android.widget.ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            // pre-condition
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(i, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
    }

    class ListAdapter extends ArrayAdapter<TwiAppPick.Apk> {

        public ListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ListAdapter(Context context, int resource, List<TwiAppPick.Apk> items) {
            super(context, resource, items);
        }

        SparseBooleanArray checked = new SparseBooleanArray();

        private boolean disposed = false;

        public Animation animation = null;

        public void dispose() {
            disposed = true;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            View row = convertView;

            if (row == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                row = vi.inflate(R.layout.app_item, null);
            }

            TwiAppPick.Apk p = getItem(position);

            if (p != null) {
                TextView name = (TextView) row.findViewById(R.id.name);
                TextView desc = (TextView) row.findViewById(R.id.desc);
                ImageView img = (ImageView) row.findViewById(R.id.img);
                CheckBox chk = (CheckBox) row.findViewById(R.id.chk);
                Button del = (Button) row.findViewById(R.id.del);
                View elem = onButtonClicked == null ? chk : del;

                if (row.getTag() == null) {
                    elem.setVisibility(View.VISIBLE);
                    row.setClickable(true);
                    row.setFocusable(true);
                    row.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            CheckBox chk = (CheckBox) view.findViewById(R.id.chk);
                            chk.toggle();
                        }
                    });

                    if (onButtonClicked == null) {
                        chk.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                            @Override
                            public void onCheckedChanged(CompoundButton checkBox, boolean flag) {
                                if (checkBox.getTag() == null || disposed) {
                                    return;
                                }
                                checked.append(getIndexFromView((View) checkBox), flag);
                            }
                        });
                    } else {
                        del.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (view.getTag() == null || disposed) {
                                    return;
                                }

                                int idx = getIndexFromView(view);
                                final TwiAppPick.Apk apk = getItem(idx);

                                final Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out);
                                animation.setAnimationListener(new Animation.AnimationListener() {
                                    @Override
                                    public void onAnimationStart(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationRepeat(Animation animation) {
                                    }

                                    @Override
                                    public void onAnimationEnd(Animation animation) {
                                        remove(apk);
                                    }
                                });

                                ((View) view.getParent().getParent()).startAnimation(animation);// remove(apk);

                                checked.delete(idx);

                                onButtonClicked.callback(apk);
                            }
                        });
                    }
                }

                name.setText(p.name);
                desc.setText(p.pkg);
                img.setImageDrawable(p.img);
                elem.setTag(null);

                if (!disposed) {
                    chk.setChecked(checked.get(position, false));
                    elem.setTag(position);
                }

            }

            return row;
        }

        public Callback<Apk> onButtonClicked;

        private int getIndexFromView(View view) {
            return (int) view.getTag();
        }

        public TwiAppPick.Apk[] getCheckedValues() {
            int count = 0;

            for (int i = 0; i < checked.size(); i++) {
                if (checked.get(checked.keyAt(i))) {
                    count++;
                }
            }

            if (count < 1) {
                return null;
            }

            TwiAppPick.Apk[] res = new TwiAppPick.Apk[count];
            int j = 0;

            for (int i = 0; i < checked.size(); i++) {
                if (!checked.get(checked.keyAt(i))) {
                    continue;
                }
                res[j++] = getItem(checked.keyAt(i));
            }

            return res;
        }

    }

    class Apk {
        public Apk(String packageName) throws PackageManager.NameNotFoundException {
            PackageManager mgr = getPackageManager();
            PackageInfo info = mgr.getPackageInfo(packageName, 0);
            name = info.applicationInfo.loadLabel(getPackageManager()).toString();
            pkg = info.packageName;
            img = info.applicationInfo.loadIcon(getPackageManager());
        }

        public String name;
        public String pkg;
        public Drawable img;
    }

}
