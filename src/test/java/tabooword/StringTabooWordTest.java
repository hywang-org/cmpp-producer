package tabooword;

import com.i.server.tabooword.core.TabooWordChecker;
import com.i.server.tabooword.core.Tree;
import org.junit.Test;

/**
 * 作者： chengli
 * 日期： 2019/10/1 17:31
 */
public class StringTabooWordTest {

    @Test
    public void StringTabooWordTest() {
        Tree tree = new Tree();
        tree.add("fuck");
        tree.add("操你妈");
        String content = "你大爷的";
        assert !TabooWordChecker.isSensitive(tree, content);
        tree.add("你大爷");
        assert TabooWordChecker.isSensitive(tree, content);

    }
}
