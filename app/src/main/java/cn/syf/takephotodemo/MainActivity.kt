package cn.syf.takephotodemo

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.jph.takephoto.app.TakePhoto
import com.jph.takephoto.app.TakePhotoImpl
import com.jph.takephoto.compress.CompressConfig
import com.jph.takephoto.model.CropOptions
import com.jph.takephoto.model.InvokeParam
import com.jph.takephoto.model.TContextWrap
import com.jph.takephoto.model.TResult
import com.jph.takephoto.permission.InvokeListener
import com.jph.takephoto.permission.PermissionManager
import com.jph.takephoto.permission.TakePhotoInvocationHandler
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File


class MainActivity : AppCompatActivity(), TakePhoto.TakeResultListener, InvokeListener {
    private var takePhoto: TakePhoto? = null
    private var invokeParam: InvokeParam? = null
    private var cropOptions: CropOptions? = null;  //裁剪参数
    private var compressConfig: CompressConfig? = null;  //压缩参数
    private var imageUri: Uri? = null;  //图片保存路径
    var TAG = "tag"
    override fun invoke(invokeParam: InvokeParam): PermissionManager.TPermissionType {
        val type = PermissionManager.checkPermission(TContextWrap.of(this), invokeParam.getMethod())
        if (PermissionManager.TPermissionType.WAIT == type) {
            this.invokeParam = invokeParam
        }
        return type
    }

    override fun takeSuccess(result: TResult) {
        Log.i(TAG, "takeSuccess：" + result.getImage().getCompressPath());
        val iconPath = result.getImage().getOriginalPath();
        //Toast显示图片路径
        Toast.makeText(this, "imagePath:" + iconPath, Toast.LENGTH_SHORT).show();
        //Google Glide库 用于加载图片资源
        Glide.with(this).load(iconPath).into(head_iv);

    }

    override fun takeCancel() {
        Log.i(TAG, getResources().getString(R.string.msg_operation_canceled));
    }

    override fun takeFail(result: TResult?, msg: String?) {
        Log.i(TAG, "takeFail:" + msg);
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        getTakePhoto()?.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        getTakePhoto()?.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun initView() {
        cropOptions = CropOptions.Builder().setAspectX(1).setAspectY(1).setWithOwnCrop(false).create();
        //设置压缩参数
        compressConfig = CompressConfig.Builder().setMaxSize(50 * 1024).setMaxPixel(800).create();
        takePhoto?.onEnableCompress(compressConfig, true);  //设置为需要压缩
        pop_pic.setOnClickListener {
            imageUri = getImageCropUri();
            //从相册中选取图片并裁剪
            takePhoto?.onPickFromGalleryWithCrop(imageUri, cropOptions);
        }

        pop_camera.setOnClickListener {
            val imageUri = getImageCropUri();
            //拍照并裁剪
            takePhoto?.onPickFromCaptureWithCrop(imageUri, cropOptions);
        }
    }

    fun getImageCropUri(): Uri {
        val file = File(Environment.getExternalStorageDirectory(), "/temp/" + System.currentTimeMillis() + ".jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        return Uri.fromFile(file);
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        getTakePhoto()!!.onActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data)

    }

    /**
     * 获取TakePhoto实例
     *
     * @return
     */
    fun getTakePhoto(): TakePhoto? {
        if (takePhoto == null) {
            takePhoto = TakePhotoInvocationHandler.of(this).bind(TakePhotoImpl(this, this)) as TakePhoto?

        }
        return takePhoto;
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val type = PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionManager.handlePermissionsResult(this, type, invokeParam, this)
    }
}
