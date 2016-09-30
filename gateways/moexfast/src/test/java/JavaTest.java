import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.google.common.math.DoubleMath;

import java.util.Comparator;

import static org.junit.Assert.assertEquals;

/**
 * Created by egore on 12/19/15.
 */
public class JavaTest {

    class TestClass {
        String id;

        double value;

        @Override
        public boolean equals(Object obj) {
            if (((TestClass) obj).id != id)
                return false;

            return super.equals(obj);
        }
    }

    public void multisetTest() {
        Multiset<TestClass> set = TreeMultiset.create(new Comparator<TestClass>() {
            @Override
            public int compare(TestClass testClass, TestClass t1) {
                int vc = Double.compare(testClass.value, t1.value);
                if (vc == 0)
                    return testClass.id.compareTo(t1.id);
                else
                    return vc;
            }
        });

        assertEquals(0, set.size());

        set.add(new TestClass() {
            {
                id = "a1";
                value = 10.0;
            }
        });

        set.add(new TestClass() {
            {
                id = "a2";
                value = 10.0;
            }
        });

        assertEquals(2, set.size());

        for (TestClass tc : set)
            System.out.println(tc.id + " " + tc.value);

    }
}
