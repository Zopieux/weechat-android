package com.ubergeek42.WeechatAndroid.fragments;

import android.support.v4.app.ListFragment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.ubergeek42.WeechatAndroid.adapters.BufferListAdapter;
import com.ubergeek42.WeechatAndroid.R;
import com.ubergeek42.WeechatAndroid.WeechatActivity;
import com.ubergeek42.WeechatAndroid.relay.Buffer;
import com.ubergeek42.WeechatAndroid.relay.BufferList;
import com.ubergeek42.WeechatAndroid.relay.BufferListEye;
import com.ubergeek42.WeechatAndroid.service.Events;
import com.ubergeek42.WeechatAndroid.service.P;

import static com.ubergeek42.WeechatAndroid.service.RelayService.STATE.*;

import de.greenrobot.event.EventBus;

public class BufferListFragment extends ListFragment implements BufferListEye, View.OnClickListener {

    private static Logger logger = LoggerFactory.getLogger("BufferListFragment");
    final private static boolean DEBUG_LIFECYCLE = false;
    final private static boolean DEBUG_MESSAGES = false;
    final private static boolean DEBUG_PREFERENCES = false;
    final private static boolean DEBUG_CLICK = false;

    private WeechatActivity activity;
    private BufferListAdapter adapter;

    private RelativeLayout uiFilterBar;
    private EditText uiFilter;
    private ImageButton uiFilterClear;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// lifecycle
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /** This makes sure that the container activity has implemented
     ** the callback interface. If not, it throws an exception. */
    @Override
    public void onAttach(Context context) {
        if (DEBUG_LIFECYCLE) logger.debug("onAttach()");
        super.onAttach(context);
        this.activity = (WeechatActivity) context;
    }

    /** Supposed to be called only once
     ** since we are setting setRetainInstance(true) */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG_LIFECYCLE) logger.debug("onCreate()");
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        if (DEBUG_LIFECYCLE) logger.debug("onCreateView()");
        View view = inflater.inflate(R.layout.bufferlist, container, false);
        uiFilter = (EditText) view.findViewById(R.id.bufferlist_filter);
        uiFilter.addTextChangedListener(filterTextWatcher);
        uiFilterClear = (ImageButton) view.findViewById(R.id.bufferlist_filter_clear);
        uiFilterClear.setOnClickListener(this);
        uiFilterBar = (RelativeLayout) view.findViewById(R.id.filter_bar);
        return view;
    }

    @Override
    public void onDestroyView() {
        if (DEBUG_LIFECYCLE) logger.debug("onDestroyView()");
        super.onDestroyView();
        uiFilter.removeTextChangedListener(filterTextWatcher);
    }

    @Override
    public void onStart() {
        if (DEBUG_LIFECYCLE) logger.debug("onStart()");
        super.onStart();
        EventBus.getDefault().registerSticky(this);
        uiFilterBar.setVisibility(P.showBufferFilter ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onStop() {
        if (DEBUG_LIFECYCLE) logger.debug("onStop()");
        EventBus.getDefault().unregister(this);
        super.onStop();
        detachFromBufferList();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// event
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("unused")
    public void onEvent(Events.StateChangedEvent event) {
        logger.debug("onEvent({})", event);
        if (event.state.contains(LISTED)) attachToBufferList();
    }

    //////////////////////////////////////////////////////////////////////////////////////////////// the juice

    private void attachToBufferList() {
        adapter = new BufferListAdapter(activity);
        BufferList.setBufferListEye(this);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setListAdapter(adapter);
            }
        });
        setFilter(uiFilter.getText());
        onBuffersChanged();
    }

    private void detachFromBufferList() {
        BufferList.setBufferListEye(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// on click
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /** this is the mother method, it actually opens buffers */
    @Override
    public void onListItemClick(ListView listView, View view, int position, long id) {
        if (DEBUG_CLICK) logger.debug("onListItemClick(..., ..., {}, ...)", position);
        Object obj = getListView().getItemAtPosition(position);
        if (obj instanceof Buffer)
            activity.openBuffer(((Buffer) obj).fullName);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// BufferListEye
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public void onBuffersChanged() {
        if (DEBUG_MESSAGES) logger.debug("onBuffersChanged()");
        adapter.onBuffersChanged();
        activity.notifyMainPagerChanged();
    }

    @Override public void onHotCountChanged() {
        if (DEBUG_MESSAGES) logger.debug("onHotCountChanged()");
        activity.updateHotCount(BufferList.getHotCount());
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////////// other
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /** TextWatcher object used for filtering the buffer list */
    private TextWatcher filterTextWatcher = new TextWatcher() {
        @Override public void afterTextChanged(Editable a) {}
        @Override public void beforeTextChanged(CharSequence arg0, int a, int b, int c) {}

        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (DEBUG_PREFERENCES) logger.debug("onTextChanged({}, ...)", s);
            if (adapter != null) {
                setFilter(s);
                adapter.onBuffersChanged();
            }
        }
    };

    private void setFilter(final CharSequence s) {
        BufferList.setFilter(s.toString());
        activity.runOnUiThread(new Runnable() {
            @Override public void run() {
                uiFilterClear.setVisibility((s.length() == 0) ? View.INVISIBLE : View.VISIBLE);
            }
        });
    }

    /** the only button we've got: clear text in the filter */
    @Override
    public void onClick(View v) {
        uiFilter.setText(null);
    }

    @Override public String toString() {
        return "BL";
    }
}
