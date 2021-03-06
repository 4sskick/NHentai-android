package moe.feng.nhentai.ui;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.Menu;
import android.view.MenuItem;

import com.google.gson.Gson;

import java.util.ArrayList;

import moe.feng.nhentai.R;
import moe.feng.nhentai.api.PageApi;
import moe.feng.nhentai.api.common.NHentaiUrl;
import moe.feng.nhentai.dao.FavoriteCategoriesManager;
import moe.feng.nhentai.dao.FavoritesManager;
import moe.feng.nhentai.model.BaseMessage;
import moe.feng.nhentai.model.Book;
import moe.feng.nhentai.model.Category;
import moe.feng.nhentai.ui.adapter.BookListRecyclerAdapter;
import moe.feng.nhentai.ui.common.AbsActivity;
import moe.feng.nhentai.ui.common.AbsRecyclerViewAdapter;
import moe.feng.nhentai.util.AsyncTask;

public class CategoryActivity extends AbsActivity {

	private FavoritesManager mFM;
	private FavoriteCategoriesManager mFCM;

	private RecyclerView mRecyclerView;
	private BookListRecyclerAdapter mAdapter;
	private StaggeredGridLayoutManager mLayoutManager;

	private SwipeRefreshLayout mSwipeRefreshLayout;

	private ArrayList<Book> mBooks;

	private int mNowPage = 1;
	private Category category;

	private boolean isFavorite = false;

	private static final String EXTRA_CATEGORY_JSON = "category_json";

	public static final String TAG = CategoryActivity.class.getSimpleName();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Intent intent = getIntent();
		category = new Gson().fromJson(intent.getStringExtra(EXTRA_CATEGORY_JSON), Category.class);

		mFM = FavoritesManager.getInstance(getApplicationContext());
		mFCM = FavoriteCategoriesManager.getInstance(getApplicationContext());

		isFavorite = mFCM.contains(category);

		setContentView(R.layout.activity_search_result);

		mActionBar.setDisplayHomeAsUpEnabled(true);

		String title = "";
		switch (category.type) {
			case Category.Type.ARTIST:
				title += getString(R.string.tag_type_artists);
				break;
			case Category.Type.CHARACTER:
				title += getString(R.string.tag_type_characters);
				break;
			case Category.Type.GROUP:
				title += getString(R.string.tag_type_group);
				break;
			case Category.Type.PARODY:
				title += getString(R.string.tag_type_parodies);
				break;
			case Category.Type.TAG:
				title += getString(R.string.tag_type_tag);
				break;
			case Category.Type.LANGUAGE:
				title += getString(R.string.tag_type_language);
				break;
		}
		title += category.name;

		mActionBar.setTitle(title);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			mActionBar.setElevation(getResources().getDimension(R.dimen.appbar_elevation));
		}

		mSwipeRefreshLayout.setRefreshing(true);
		new PageGetTask().execute(mNowPage);
	}

	@Override
	protected void setUpViews() {
		mRecyclerView = $(R.id.recycler_view);
		mSwipeRefreshLayout = $(R.id.swipe_refresh_layout);

		mLayoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
		mRecyclerView.setLayoutManager(mLayoutManager);
		mRecyclerView.setHasFixedSize(true);

		mBooks = new ArrayList<>();
		mAdapter = new BookListRecyclerAdapter(mRecyclerView, mBooks, mFM);
		setRecyclerViewAdapter(mAdapter);

		mSwipeRefreshLayout.setColorSchemeResources(
				R.color.deep_purple_500, R.color.pink_500, R.color.orange_500, R.color.brown_500,
				R.color.indigo_500, R.color.blue_500, R.color.teal_500, R.color.green_500
		);
		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				if (!mSwipeRefreshLayout.isRefreshing()) {
					mSwipeRefreshLayout.setRefreshing(true);
				}

				mBooks = new ArrayList<>();
				mAdapter = new BookListRecyclerAdapter(mRecyclerView, mBooks, mFM);
				setRecyclerViewAdapter(mAdapter);
				new PageGetTask().execute(mNowPage = 1);
			}
		});
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		getMenuInflater().inflate(R.menu.menu_details, menu);

		MenuItem mFavItem = menu.findItem(R.id.action_favorite);
		mFavItem.setIcon(isFavorite ? R.drawable.ic_favorite_white_24dp : R.drawable.ic_favorite_outline_white_24dp);
		mFavItem.setTitle(isFavorite ? R.string.action_favorite_true : R.string.action_favorite_false);

		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == R.id.action_favorite) {
			if (isFavorite) {
				mFCM.remove(mFCM.find(category));
			} else {
				mFCM.add(category);
			}
			mFCM.save();
			isFavorite = !isFavorite;
			Snackbar.make(
					mRecyclerView,
					isFavorite ? R.string.favorite_add_finished : R.string.favorite_remove_finished,
					Snackbar.LENGTH_LONG
			).show();
			invalidateOptionsMenu();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void setRecyclerViewAdapter(BookListRecyclerAdapter adapter) {
		adapter.setOnItemClickListener(new AbsRecyclerViewAdapter.OnItemClickListener() {
			@Override
			public void onItemClick(int position, AbsRecyclerViewAdapter.ClickableViewHolder viewHolder) {
				BookListRecyclerAdapter.ViewHolder holder = (BookListRecyclerAdapter.ViewHolder) viewHolder;
				BookDetailsActivity.launch(CategoryActivity.this, holder.mPreviewImageView, holder.book, position);
			}
		});
		adapter.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(RecyclerView rv, int dx, int dy) {
				if (!mSwipeRefreshLayout.isRefreshing() && mLayoutManager.findLastCompletelyVisibleItemPositions(new int[2])[1] >= mAdapter.getItemCount() - 2) {
					mSwipeRefreshLayout.setRefreshing(true);
					new PageGetTask().execute(++mNowPage);
				}
			}
		});

		mRecyclerView.setAdapter(adapter);
	}

	private class PageGetTask extends AsyncTask<Integer, Void, BaseMessage> {

		@Override
		protected BaseMessage doInBackground(Integer... params) {
			mFM.reload();
			return PageApi.getPageList(NHentaiUrl.getCategoryUrl(category) + "/?page=" + mNowPage);
		}

		@Override
		protected void onPostExecute(BaseMessage msg) {
			mSwipeRefreshLayout.setRefreshing(false);
			if (msg != null) {
				if (msg.getCode() == 0 && msg.getData() != null) {
					if (!((ArrayList<Book>) msg.getData()).isEmpty()) {
						mBooks.addAll((ArrayList<Book>) msg.getData());
						mAdapter.notifyDataSetChanged();
						if (mNowPage == 1) {
							mRecyclerView.setAdapter(mAdapter);
						}
					} else {
						Snackbar.make(mRecyclerView, R.string.tips_no_result, Snackbar.LENGTH_LONG).show();
					}
				} else if (mNowPage == 1) {
					Snackbar.make(mRecyclerView, R.string.tips_no_result, Snackbar.LENGTH_LONG).show();
				}
			}
		}

	}

	public static void launch(AppCompatActivity activity, Category category) {
		Intent intent = new Intent(activity, CategoryActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_CATEGORY_JSON, category.toJSONString());
		activity.startActivity(intent);
	}

}
