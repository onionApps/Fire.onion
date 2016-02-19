/*
 * Fire.onion
 *
 * https://play.google.com/store/apps/details?id=onion.fire
 * http://onionapps.github.io/Fire.onion/
 * http://github.com/onionApps/Fire.onion
 *
 * Author: http://github.com/onionApps - http://jkrnk73uid7p5thz.onion - bitcoin:1kGXfWx8PHZEVriCNkbP5hzD15HS4AyKf
 */

package onion.fire;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadView extends FrameLayout {

    String TAG = "DownloadView";

    List<DownloadManager.Download> downloads = new ArrayList<>();

    class DownloadHolder extends RecyclerView.ViewHolder {
        TextView name, status;
        ProgressBar progress;

        public DownloadHolder(View v) {
            super(v);
            name = (TextView) v.findViewById(R.id.name);
            status = (TextView) v.findViewById(R.id.status);
            progress = (ProgressBar) v.findViewById(R.id.progress);
        }
    }

    class DownloadAdapter extends RecyclerView.Adapter<DownloadHolder> {
        @Override
        public int getItemCount() {
            return downloads.size();
        }

        @Override
        public DownloadHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new DownloadHolder(LayoutInflater.from(getContext()).inflate(R.layout.download_item, parent, false));
        }

        @Override
        public void onBindViewHolder(DownloadHolder holder, final int position) {
            if (position < 0 || position >= getItemCount())
                return;
            final DownloadManager.Download download = downloads.get(getItemCount() - 1 - position);
            holder.name.setText(download.getFile().getName());
            //holder.status.setText(download.getProgress() + " / " + download.getSize());
            if (download.getSize() < 0) {
                holder.progress.setProgress(50);
                holder.progress.setIndeterminate(true);
                holder.progress.setVisibility(View.VISIBLE);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                //holder.status.setText(download.getProgress() == 0 ? "Preparing..." : "" + download.getProgress());
                holder.status.setVisibility(View.GONE);
            } else if (download.getSize() != download.getProgress()) {
                holder.progress.setProgress((int) (download.getProgress() * 100 / download.getSize()));
                holder.progress.setIndeterminate(false);
                holder.progress.setVisibility(View.VISIBLE);
                holder.itemView.setOnClickListener(null);
                holder.itemView.setOnLongClickListener(null);
                //holder.status.setText("" + download.getProgress() + " / " + download.getSize());
                holder.status.setVisibility(View.GONE);
            } else {
                holder.progress.setVisibility(View.GONE);
                holder.progress.setProgress(50);
                holder.progress.setIndeterminate(false);
                holder.itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        open(download);
                    }
                });
                holder.itemView.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        new AlertDialog.Builder(getContext())
                                .setTitle(download.getFile().getName())
                                .setMessage(download.getFile().toString())
                                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        open(download);
                                    }
                                })
                                .setNeutralButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        DownloadManager.getInstance(getContext()).removeDownload(download);
                                        init();
                                        Toast.makeText(getContext(), "File deleted.", Toast.LENGTH_SHORT).show();
                                    }
                                })
                                .show();
                        return true;
                    }
                });
                holder.status.setText(download.getError() != null ? "Error: " + download.getError() : "");
                holder.status.setVisibility(download.getError() != null ? View.VISIBLE : View.GONE);
            }
        }
    }

    void open(DownloadManager.Download download) {

        File file = download.getFile();
        Log.i(TAG, "onClick: " + file);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //String mimeType = myMime.getMimeTypeFromExtension(fileExt(getFile()).substring(1));
        //newIntent.setDataAndType(Uri.fromFile(getFile()),mimeType);
        //intent.setData(Uri.fromFile(file));

        Uri uri;

        if (download.isExternal())
            uri = Uri.fromFile(file);
        else
            uri = DownloadProvider.getInstance().getUri(file);

        Log.i(TAG, "uri " + uri);

        intent.setData(uri);

        String name = file.getName();
        int exti = name.lastIndexOf(".");
        if (exti > 0) {
            String ext = name.substring(exti + 1);
            Log.i(TAG, "ext " + ext);
            String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            Log.i(TAG, "mime " + mime);
            if (mime != null) {
                //intent.setType(mime);
                intent.setDataAndType(uri, mime);
            }
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            getContext().startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(getContext(), "Failed to open file.", Toast.LENGTH_LONG).show();
        }

    }

    DownloadAdapter adapter = new DownloadAdapter();

    RecyclerView recyclerView;
    View emptyView;

    public DownloadView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        instance = this;

        recyclerView = new RecyclerView(context);
        addView(recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        emptyView = inflate(getContext(), R.layout.download_empty, null);
        addView(emptyView);

        init();
    }

    static DownloadView instance;

    public static void update() {
        DownloadView v = instance;
        if (v != null) {
            v.update2();
        }
    }

    private void update2() {
        post(new Runnable() {
            @Override
            public void run() {
                init();
            }
        });
    }

    private void init() {
        downloads = DownloadManager.getInstance(getContext()).getDownloads();
        emptyView.setVisibility(downloads.size() == 0 ? View.VISIBLE : View.GONE);
        adapter.notifyDataSetChanged();
    }

}
