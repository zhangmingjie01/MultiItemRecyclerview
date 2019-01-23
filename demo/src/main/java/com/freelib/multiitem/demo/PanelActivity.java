package com.freelib.multiitem.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.freelib.multiitem.adapter.BaseItemAdapter;
import com.freelib.multiitem.adapter.holder.BaseViewHolder;
import com.freelib.multiitem.adapter.holder.BaseViewHolderManager;
import com.freelib.multiitem.demo.bean.ImageTextBean;
import com.freelib.multiitem.demo.bean.TextDragBean;
import com.freelib.multiitem.demo.viewholder.ImageAndTextManager;
import com.freelib.multiitem.demo.viewholder.TextViewDragManager;
import com.freelib.multiitem.helper.ItemDragHelper;
import com.freelib.multiitem.helper.ViewScaleHelper;
import com.freelib.multiitem.item.UniqueItemManager;
import com.freelib.multiitem.listener.OnItemClickListener;
import com.freelib.multiitem.listener.OnItemDragListener;
import com.freelib.multiitem.listener.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PanelActivity extends AppCompatActivity {

    protected View contentView;

    public static final int NONE = -1;
    private RecyclerView horizontalRecycler;
    private BaseItemAdapter adapter;
    private ItemDragHelper dragHelper;
    private ViewScaleHelper scaleHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panel);
        contentView = findViewById(R.id.panel_content);
        initView();
    }

    protected void initView() {
        setTitle(R.string.panel_title);

        horizontalRecycler = (RecyclerView) findViewById(R.id.recyclerView);

        adapter = new BaseItemAdapter();
        //此处为了简单所以使用不可复用的模式，正式业务视具体情况而定！！！
        adapter.addDataItems(Arrays.asList(new UniqueItemManager(new RecyclerViewManager(4)),
                new UniqueItemManager(new RecyclerViewManager(8)), new UniqueItemManager(new RecyclerViewManager(4)),
                new UniqueItemManager(new RecyclerViewManager(12)), new UniqueItemManager(new RecyclerViewManager(16))));
        //设置横向滚动LinearLayoutManager
        horizontalRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        horizontalRecycler.setAdapter(adapter);

        //ItemDragHelper，需要传入外层的横向滚动的RecyclerView
        dragHelper = new ItemDragHelper(horizontalRecycler);
        dragHelper.setOnItemDragListener(new OnBaseDragListener());

        scaleHelper = new ViewScaleHelper();
        //设置最外层的Content视图
        scaleHelper.setContentView(contentView);
        //设置横向的Recycler列表视图
        scaleHelper.setHorizontalView(horizontalRecycler);

        //监听横向滚动RecyclerView双击事件，并开启关闭缩放模式
        doubleTapToggleScale();
    }

    private void doubleTapToggleScale() {
        GestureDetector doubleTapGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                scaleHelper.toggleScaleModel();
                return super.onDoubleTap(e);
            }
        });
        horizontalRecycler.setOnTouchListener((v, event) -> doubleTapGesture.onTouchEvent(event));
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        //需要把touch事件传给dragHelper，true表示消耗掉事件
        //需要保证在Activity或者外层的ViewGroup或可以拦截Touch事件的地方回调都可以
        return dragHelper.onTouch(ev) || super.dispatchTouchEvent(ev);
    }

    class OnBaseDragListener extends OnItemDragListener {

        @Override
        public float getScale() {
            return scaleHelper.isInScaleMode() ? scaleHelper.getScale() : super.getScale();
        }

        @Override
        public void onDragFinish(RecyclerView recyclerView, int itemRecyclerPos, int itemPos) {
            super.onDragFinish(recyclerView, itemRecyclerPos, itemPos);
            String text = String.format("拖动起始第%s个列表的第%s项 结束第%s个列表的第%s项 \n\n拖动数据:%s", originalRecyclerPosition,
                    originalItemPosition, itemRecyclerPos, itemPos, dragItemData);
            Toast.makeText(PanelActivity.this, text, Toast.LENGTH_SHORT).show();
        }
    }

    class RecyclerViewManager extends BaseViewHolderManager<UniqueItemManager> {
        private int length;

        RecyclerViewManager(int length) {
            this.length = length;
        }

        @Override
        protected void onCreateViewHolder(@NonNull BaseViewHolder holder) {
            super.onCreateViewHolder(holder);
            View view = holder.itemView;
            view.getLayoutParams().width = -1;

            scaleHelper.addVerticalView(view);
            final RecyclerView recyclerView = getView(view, R.id.item_group_recycler);
//            horizontalRecycler.setClipToPadding(false);

            recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
            final BaseItemAdapter baseItemAdapter = new BaseItemAdapter();
            //为XXBean数据源注册XXHolderManager管理类 数据源必须实现ItemData接口
            baseItemAdapter.register(TextDragBean.class, new TextViewDragManager());
            baseItemAdapter.register(ImageTextBean.class, new ImageAndTextManager());
            baseItemAdapter.setDataItems(getItemList(length));
            recyclerView.setAdapter(baseItemAdapter);

            baseItemAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
                @Override
                protected void onItemLongClick(BaseViewHolder viewHolder) {
                    dragHelper.startDrag(viewHolder);
                }
            });
            baseItemAdapter.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(BaseViewHolder viewHolder) {

                    Toast.makeText(PanelActivity.this, "你点击了" + viewHolder.getItemPosition() + "---" + viewHolder.getItemData(), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onBindViewHolder(@NonNull BaseViewHolder holder, @NonNull UniqueItemManager data) {
            TextView groupTxt = getView(holder.itemView, R.id.item_group_name);
            groupTxt.setText("待办任务组" + holder.getItemPosition());
        }

        @Override
        protected int getItemLayoutId() {
            return R.layout.item_recycler_view;
        }

        private List<Object> getItemList(int length) {
            List<Object> list = new ArrayList<>();
            TextDragBean textDragBean;
            for (int i = 0; i < length; i++) {

                switch (i % 4) {
                    case 0:
                        list.add(new TextDragBean("无限制可以自由拖动\n内容A\n内容B\n内容C"));
                        break;
                    case 1:
                        list.add(new ImageTextBean(R.drawable.img2, "事项：\n无限制自由拖动\n更多内容" + i));
                        break;
                    case 2:
                        list.add(new TextDragBean("事项：\n可以被移动\n不可被切换RecyclerView", true, false, true));
                        break;
                    case 3:
                        list.add(new TextDragBean("事项：\n不可以被移动\n不可被切换RecyclerView", false, false, true));
                        break;

                }
            }
            return list;
        }
    }
}