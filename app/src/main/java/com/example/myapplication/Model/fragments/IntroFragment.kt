import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R

class IntroFragment : Fragment() {

    companion object {
        private const val ARG_LAYOUT_ID = "layout_id"

        fun newInstance(layoutId: Int) = IntroFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_LAYOUT_ID, layoutId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val layoutId = arguments?.getInt(ARG_LAYOUT_ID) ?: R.layout.fragment_intro
        return inflater.inflate(layoutId, container, false)
    }
}
