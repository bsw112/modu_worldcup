package com.manta.worldcup.activity.fragment.dialog

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.manta.worldcup.R
import com.manta.worldcup.adapter.CommentAdapter
import com.manta.worldcup.helper.Constants
import com.manta.worldcup.model.Comment
import com.manta.worldcup.model.PictureModel
import com.manta.worldcup.model.User
import com.manta.worldcup.viewmodel.CommentViewModel
import com.manta.worldcup.viewmodel.PictureViewModel
import com.manta.worldcup.viewmodel.TopicViewModel
import com.skydoves.balloon.ArrowConstraints
import com.skydoves.balloon.ArrowOrientation
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import kotlinx.android.synthetic.main.dialog_mypicture_info.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.*

/**
 * 내가 내가 올린 사진을 클릭했을때 사진 정보를 보여주는 다이어로그다.
 * PictureInfoDialog와는 다르게, 사진의 이름이 수정가능하다.
 */
class MyPictureInfoDialog : DialogFragment() {

    private val mCommentViewModel: CommentViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return CommentViewModel(requireActivity().application) as T; }
        }).get(CommentViewModel::class.java);
    }

    private val mTopicViewModel: TopicViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return TopicViewModel(requireActivity().application) as T;
            }
        }).get(TopicViewModel::class.java)
    }

    private val mPictureViewModel: PictureViewModel by lazy {
        ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return PictureViewModel(requireActivity().application) as T;
            }
        }).get(PictureViewModel::class.java)
    }


    interface OnPictureNameChangeListener : Serializable{
        fun onPictureNameChange(pictureName : String);
    }

    private var mTopicImageNames = mutableSetOf<String>()

    private val mCommentAdapter = CommentAdapter();

    /**
     * 현재 유저가 지정한 부모댓글
     */
    private var mParentComment: Comment? = null;

    /**
     * @param picture 다이어로그에 띄울 사진의 정보
     * @param user  현재 로그인한 유저
     */
    fun newInstance(picture: PictureModel, user: User, onPictureNameChangeListener: OnPictureNameChangeListener): MyPictureInfoDialog {
        val args = Bundle(3);
        args.putSerializable(Constants.EXTRA_PICTURE_MODEL, picture);
        args.putSerializable(Constants.EXTRA_USER, user);
        args.putSerializable(Constants.EXTRA_PICTURE_NAME_CHANGE_LISTENER, onPictureNameChangeListener)
        val fragment = MyPictureInfoDialog();
        fragment.arguments = args;
        return fragment;
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.dialog_mypicture_info, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //인풋모드 설정 (EditText가 키보드에 가려지지않게)
        requireActivity().window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        val pictureModel = requireArguments().getSerializable(Constants.EXTRA_PICTURE_MODEL) as? PictureModel ?: return;
        val user = requireArguments().getSerializable(Constants.EXTRA_USER) as? User ?: return;

        rv_comment.adapter = mCommentAdapter;
        rv_comment.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false);
        mCommentAdapter.setOnItemClickListener(object : CommentAdapter.OnItemClickListener {
            override fun OnItemClick(comment: Comment, isCheckedAsParent: Boolean) {
//                //선택한 댓글이 부모로 설정되었으면 부모로 저장
//                if(isCheckedAsParent){
//                    tv_reply_to.text = comment.mWriter;
//                    tv_reply_to.visibility = View.VISIBLE;
//                    mParentComment = comment.copy();
//                }else{
//                    tv_reply_to.visibility = View.INVISIBLE;
//                }
            }
        })


        mCommentViewModel.mComments.observe(this, androidx.lifecycle.Observer {
            mCommentAdapter.setComments(it);
        })
        mCommentViewModel.getPictureComment(pictureModel.mId);

        //댓글 제출
        btn_send.setOnClickListener {
            val date = Calendar.getInstance().time;
            val locale = requireContext().applicationContext.resources.configuration.locale;
            val comment = Comment(
                0, user.mNickname, user.mEmail, et_comment.text.toString(),
                SimpleDateFormat("yyyy.MM.dd HH:mm", locale).format(date), pictureModel.mId, pictureModel.mOwnerEmail
            )
            mCommentViewModel.insertPictureComment(comment);
            //작성 후 덧글창 비우기
            et_comment.setText("");
        }


        //소속토픽 세팅
        CoroutineScope(Dispatchers.IO).launch {
            val result = mTopicViewModel.getTopicName(pictureModel.mTopicId);
            withContext(Dispatchers.Main) {
                tv_from.text = "\t" + result;
            }
        }

        //우승횟수
        tv_winCnt.text = "\t" + pictureModel.WinCnt.toString()

        //사진셋팅
        val url = Constants.BASE_URL + "image/get/${pictureModel.mId}/";
        Constants.GlideWithHeader(url, iv_picture, iv_picture, requireContext());

        //사진 이름 셋팅
        tv_picture_name.text = pictureModel.mPictureName;

        tv_income.text = "\t" + (pictureModel.WinCnt * Constants.POINT_WIN_PICTURE)

        btn_info.setOnClickListener {
            val balloon: Balloon = Balloon.Builder(requireContext())
                .setArrowSize(10)
                .setArrowOrientation(ArrowOrientation.BOTTOM)
                .setArrowConstraints(ArrowConstraints.ALIGN_ANCHOR)
                .setPadding(6)
                .setArrowPosition(0.5f)
                .setCornerRadius(10f)
                .setBackgroundColorResource(R.color.yellow)
                .setTextColorResource(R.color.black)
                .setText(resources.getString(R.string.tooltip_picture_income) + " " + Constants.POINT_WIN_PICTURE)
                .setBalloonAnimation(BalloonAnimation.FADE)
                .build()

            balloon.show(btn_info)
        }

        //사진이 등록된 토픽에 있는 사진들의 이름목록 가져오기
        CoroutineScope(Dispatchers.IO).launch {
            val result = mPictureViewModel.getTopicImageNames(pictureModel.mTopicId)
            if (result.isSuccessful) {
                mTopicImageNames = result.body()!!
            }
        }

        //사진 이름 바꾸기
        btn_change_picture_name.setOnClickListener {
            if(mTopicImageNames.isEmpty()){
                Toast.makeText(context, resources.getString(R.string.warn_error), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            PictureDescriptionDialog().newInstance(ArrayList(mTopicImageNames), pictureModel.mPictureName ,object : PictureDescriptionDialog.OnSubmitListener {
                override fun onSubmit(pictureName: String) {
                    if(pictureName != pictureModel.mPictureName){
                        tv_picture_name.text = pictureName;
                        mPictureViewModel.updatePictureName(pictureModel.mId, pictureName)
                        val onPictureNameChange = arguments?.getSerializable(Constants.EXTRA_PICTURE_NAME_CHANGE_LISTENER) as? OnPictureNameChangeListener
                        onPictureNameChange?.onPictureNameChange(pictureName)
                    }
                }
            }).show(requireFragmentManager(), null)
        }
    }


}