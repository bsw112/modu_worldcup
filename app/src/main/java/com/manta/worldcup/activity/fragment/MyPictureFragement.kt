package com.manta.worldcup.activity.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.GridLayoutManager
import com.manta.worldcup.R
import com.manta.worldcup.adapter.MyPictureAdapter
import com.manta.worldcup.helper.Constants
import com.manta.worldcup.viewmodel.PictureViewModel
import com.manta.worldcup.viewmodel.UserViewModel
import kotlinx.android.synthetic.main.frag_mypicture.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 내 사진을 볼 수 있는 프래그먼트
 */
class MyPictureFragement() : Fragment(R.layout.frag_mypicture) {
    private lateinit var mUserViewModel: UserViewModel;
    private lateinit var mMyPictureAdapter: MyPictureAdapter;

    private val mPictureModel: PictureViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return PictureViewModel(requireActivity().application) as T;
            }
        }).get(PictureViewModel::class.java);
    }


    private val mRefreshReceiver = object : BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    /**
     * @param mNotifiedPictureId : notificationBanner를 클릭했을때 전달받은 pictureId
     */
    fun newInstance(mNotifiedPictureId: String?): MyPictureFragement {
        val args = Bundle(1)
        args.putString(Constants.EXTRA_NOTIFIED_PICTURE_ID, mNotifiedPictureId)
        val fragment = MyPictureFragement()
        fragment.arguments = args
        return fragment
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mUserViewModel = ViewModelProvider(requireActivity(), object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return UserViewModel(requireActivity().application) as T;
            }

        }).get(UserViewModel::class.java);

        mMyPictureAdapter = MyPictureAdapter(
            requireActivity().supportFragmentManager,
            arguments?.getString(Constants.EXTRA_NOTIFIED_PICTURE_ID)
        )

        rv_picture.layoutManager = GridLayoutManager(context, 2)
        rv_picture.adapter = mMyPictureAdapter;

        mUserViewModel.mUser.observe(this, Observer { user ->
            mMyPictureAdapter.setUser(user);
        })

        mUserViewModel.mPictures.observe(this, Observer {
            mMyPictureAdapter.setPictures(ArrayList(it));
        })

        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mRefreshReceiver, IntentFilter(Constants.ACTION_NEED_REFRESH))

        //옵션바

        val showAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bottom_option_show)
        val dismissAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.anim_bottom_option_dismiss)
        dismissAnim.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationRepeat(animation: Animation?) {

            }

            override fun onAnimationEnd(animation: Animation?) {
                ll_option_mypicture.visibility = View.INVISIBLE;
            }

            override fun onAnimationStart(animation: Animation?) {

            }

        })

        ll_option_mypicture.setOnTouchListener { v, event ->
            ll_option_mypicture.performClick();
            //밑에 있는 뷰의 클릭 막기 (터치이벤트 consume)
            true;
        }

        mMyPictureAdapter.setOnLongClickListener(object : MyPictureAdapter.OnItemLongClickListener {
            override fun onItemLongClick() {
                ll_option_mypicture.visibility = View.VISIBLE;
                ll_option_mypicture.startAnimation(showAnim)
            }
        })

        btn_delete.setOnClickListener {
            val selection = mMyPictureAdapter.getSelection();
            CoroutineScope(Dispatchers.IO).launch {
                val result = mPictureModel.deletePictures(selection)
                if (result.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        mMyPictureAdapter.endSelectMode()
                        LocalBroadcastManager.getInstance(requireContext()).sendBroadcast(
                            Intent().apply { action = Constants.ACTION_NEED_REFRESH }
                        )
                    }
                } else {
                    Toast.makeText(requireContext(), resources.getString(R.string.warn_error), Toast.LENGTH_SHORT).show()
                }
            }

            ll_option_mypicture.visibility = View.INVISIBLE;
        }

        btn_select_all.setOnClickListener {
            mMyPictureAdapter.selectAll()
        }
        btn_cancel.setOnClickListener {
            mMyPictureAdapter.endSelectMode()
            ll_option_mypicture.startAnimation(dismissAnim)
        }


        refresh()

    }


    override fun onDestroy() {
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mRefreshReceiver);
        super.onDestroy()

    }

    private fun refresh() {
        //내 사진을 가져온다.
        mUserViewModel.getAllPicture()
    }


}