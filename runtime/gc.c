#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/time.h>

// The Gimple Garbage Collector.


//===============================================================//
// The Java Heap data structure.

/*   
      ----------------------------------------------------
      |                        |                         |
      ----------------------------------------------------
      ^\                      /^
      | \<~~~~~~~ size ~~~~~>/ |
    from                       to
 */
struct JavaHeap
{
  int size;         // in bytes, note that this if for semi-heap size
  char *from;       // the "from" space pointer
  char *fromFree;   // the next "free" space in the from space
  char *to;         // the "to" space pointer
  char *toStart;    // "start" address in the "to" space
  char *toNext;     // "next" free space pointer in the to space
};

// The Java heap, which is initialized by the following
// "heap_init" function.
struct JavaHeap heap;

// Lab 4, exercise 10:
// Given the heap size (in bytes), allocate a Java heap
// in the C heap, initialize the relevant fields.
void Tiger_heap_init (int heapSize)
{
  // You should write 7 statement here:
  // #1: allocate a chunk of memory of size "heapSize" using "malloc"
    char *java_Heap = (char *)malloc(heapSize);
    memset(java_Heap,0,heapSize);
  // #2: initialize the "size" field, note that "size" field
    heap.size = heapSize/2;
  // is for semi-heap, but "heapSize" is for the whole heap.
    
  // #3: initialize the "from" field (with what value?)
    heap.from = java_Heap;
  // #4: initialize the "fromFree" field (with what value?)
    heap.fromFree = heap.from;
  // #5: initialize the "to" field (with what value?)
    heap.to = java_Heap + heap.size;
  // #6: initizlize the "toStart" field with NULL;
    heap.toStart = heap.to;
  // #7: initialize the "toNext" field with NULL;
    heap.toNext = heap.toStart;
}

// The "prev" pointer, pointing to the top frame on the GC stack. 
// (see part A of Lab 4)
extern void *prev;
static void Tiger_gc ();

//===============================================================//
// Object Model And allocation


// Lab 4: exercise 11:
// "new" a new object, do necessary initializations, and
// return the pointer (reference).
/*    ----------------
 p--->| vptr      ---|----> (points to the virtual method table)
      |--------------|
      | isObjOrArray | (0: for normal objects)
      |--------------|
      | length       | (this field should be empty for normal objects)
      |--------------|
      | forwarding   | 
      |--------------|\
      | v_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | v_{size-1}   | /e
      ----------------/
*/
// Try to allocate an object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new (void *vtable, int size)
{
  // Your code here:
    int *p = 0;
    if((heap.fromFree + size) > (heap.from + heap.size)) 
    {
       Tiger_gc();
    }
    if((heap.fromFree + size) > (heap.from + heap.size))
    {
       printf("OutOfMemory\n");
       exit(0);
    }
    else 
    {
       p = (int *)heap.fromFree;
       heap.fromFree = heap.fromFree + size;
       memset((char *)p,0,size);
       *p = (int)vtable;
       *(p+1) = 0;
       *(p+2) = 0;
       *(p+3) = 0;
    }
    return p;
}

// "new" an array of size "length", do necessary
// initializations. And each array comes with an
// extra "header" storing the array length and other information.
/*    ----------------
 p--->| vptr         | (this field should be empty for an array)
      |--------------|
      | isObjOrArray | (1: for array)
      |--------------|
      | length       |
      |--------------|
      | forwarding   | 
      |--------------|\
      | e_0          | \      
      |--------------|  s
      | ...          |  i
      |--------------|  z
      | e_{length-1} | /e
      ----------------/
*/
// Try to allocate an array object in the "from" space of the Java
// heap. Read Tiger book chapter 13.3 for details on the
// allocation.
// There are two cases to consider:
//   1. If the "from" space has enough space to hold this array object, then
//      allocation succeeds, return the apropriate address (look at
//      the above figure, be careful);
//   2. if there is no enough space left in the "from" space, then
//      you should call the function "Tiger_gc()" to collect garbages.
//      and after the collection, there are still two sub-cases:
//        a: if there is enough space, you can do allocations just as case 1; 
//        b: if there is still no enough space, you can just issue
//           an error message ("OutOfMemory") and exit.
//           (However, a production compiler will try to expand
//           the Java heap.)
void *Tiger_new_array (int length)
{
  // Your code here:
  int *p = 0; 
  if((heap.fromFree + (length + 4) * sizeof(int)) > (heap.from + heap.size)) 
  {
    //  printf("Tiger_new_array\n");
      Tiger_gc ();
  }
  if((heap.fromFree + (length + 4)* sizeof(int)) > (heap.from + heap.size)) 
  {
      printf("OutOfMemory\n");
      exit(0);
  }
  else 
  {
     p = (int *)heap.fromFree;
     memset(heap.fromFree,0,(length + 4) * sizeof(int));
     heap.fromFree = heap.fromFree + (length + 4) * sizeof(int);
     *p = 0;
     *(p+1) = 1;
     *(p+2) = length;
     *(p+3) = 0;
  }
  return p;
}

//===============================================================//
// The Gimple Garbage Collector

// Lab 4, exercise 12:
// A copying collector based-on Cheney's algorithm.
void gc_handle_normal_obj(int *);
void gc_handle_array_obj(int *);
void gc_handle_normal_obj(int *obj)
{
    int *toNext = (int *)heap.toNext;
    //toNext的第一项是对象的虚函数表指针
    *toNext = *obj;
    //虚函数表的第一项为类的gc_map，暗含了类中属性变量的多少
    char *class_gc_map = (char *)(*(int *)(*obj));
    int size = strlen(class_gc_map);
   /*int size = 0;
     while(*class_gc_map != '\0')
     {
         size++;
         class_gc_map++;
     }*/ 
    //移动toNext
  //  printf("+++size+++%d",size);
   // printf("before--%x--\n",heap.toNext);
    heap.toNext = heap.toNext + sizeof(int) * (4 + size);
   // printf("after--%x--\n",heap.toNext);
    *(toNext + 1) = 0;
    *(toNext + 2) = 0;
    //from中对象的forwarding置为该对象转到to后的新地址
    *(obj + 3) = toNext;
    //to中对象的forwarding重置为0
    *(toNext + 3) = 0;
    //处理对象的size部分
    int i;
    for(i = 0; i < size; i++)
    {
      //对象的size部分完全拷贝到to堆
      *(toNext + 4 + i) = *(obj + 4 + i);    
    //  printf("-----------111-----%d", *(toNext + 4 + i));
    }
    for(i = 0; i < size; i++)
    {
      //对象的size部分完全拷贝到to堆
     // *(toNext + 4 + i) = *(obj + 4 + i);
      //如果类的属性是指针,继续进行垃圾回收
      if(*(class_gc_map + i) == '1')
      {  
          //p就是属性   
          int *p = (int *)(*(toNext + 4 + i));
          if(p < heap.from || p > (heap.from + heap.size))
          {
             printf("OutofMemory\n");
             exit(1);
          }
          //如果是一个普通类型的对象
          if(*(p + 1) == 0)
          {
            //pan duan shifou yijing kaobei
            if(*(p + 3) == 0)
            {
              *(toNext + 4 + i) = (int *)heap.toNext;
              gc_handle_normal_obj(p);
            }
          }
          //如果是数组型对象
          if(*(p + 1) == 1)
          {
            //pan duan shifou yijing kaobei
            if(*(p + 3) == 0)
            {
              //kai shi kao bei zhi qian,ba jiang lai de di zhi gei dui 
      //xiang de shu xing
              *(toNext + 4 + i) = (int *)heap.toNext;
              gc_handle_array_obj(p);
            }
          }
      } 
    }
    //print message
   // printf("---normalObj--toNext---heap.toNext:----\n");
   // printf("--%x--\n",heap.toNext);
    return;
}

void gc_handle_array_obj(int *obj)
{
  int *toNext = (int *)heap.toNext;
  int size = *(obj + 2);
  //移动toNext指针
  //printf("arraybefore--%x--\n",heap.toNext);
  heap.toNext = heap.toNext + sizeof(int) * (4 + size);
  //printf("arrayafter--%x--\n",heap.toNext);
  *(toNext) = *obj;
  *(toNext + 1) = 1;
  *(toNext + 2) = size;
  //from中对象的forwarding置为该对象转到to后的新地址
  *(obj + 3) = toNext;
  //to中对象的forwarding重置为0
  *(toNext + 3) = 0;
  //处理对象的size部分
  int i;
  for(i = 0; i < size; i++)
  {
     //对象的size部分完全拷贝到to堆
     *(toNext + 4 + i) = *(obj + 4 + i);
  }
   //print message
  // printf("----arrayObj----toNext-- -heap.toNext:----\n");
  // printf("--%x--\n",heap.toNext);
}
int j = 0;
static void Tiger_gc ()
{
  struct timeval t_start, t_end;
  gettimeofday(&t_start, NULL);
  j++;
  //printf("--gc di %d ci ------gc_Start------\n",j);
  // Your code here:
  int *gc_top = (int *)prev;  
  while(gc_top != NULL)
  {
     //处理方法参数中的引用参数
     char *p = (char *)(*(gc_top + 1));
     int *arguments_base_address = (int *)(*(gc_top + 2));
     int *q = arguments_base_address;
     int *s = 0;
     int i = 0;
     while(*p != '\0' && q != 0)
     {
        int *arg = q + i;
        if(*p == '1')
        {
           int *k = (int *)(*arg);
           if((k >= ((int *)(heap.from))) && (k < ((int *)(heap.from + heap.size))))
           {
              s = k;
              s = s + 1;
              if(*s == 0)
              {
                 if(*(s+2) == 0)
                 {
                   *arg = heap.toNext;
                   gc_handle_normal_obj(k);
                 }
              }
              if(*s == 1)
              {
                 if(*(s+2) == 0)
                 {
                   *arg = heap.toNext;
                   gc_handle_array_obj(k);
                 }
              }
           }
        }
        i++;
        p++;
     }
     //处理方法局部变量中是引用的变量
     p = (char *)(*(gc_top + 3));
     char *p1 = p;
     int local_num = 0;
     //计算方法局部变量中是引用的变量的个数
     while(*p1 != '\0')
     {
        if(*p1 == '1')
        {
           local_num++;
        }
        p1++;
     }
     q = gc_top + 3;   
     for(i = 0; i < local_num; i++)
     {
        q = q + 1 + i;
       // //对象的size部分拷贝到to
        int *isObjOrArray = (int *)(*q) + 1;
        if(*isObjOrArray == 0)
        {
           if(*(isObjOrArray + 2) == 0)
           {
             int *s = (int *)(*q);
             *q = (int *)heap.toNext;
             gc_handle_normal_obj(s);
           }
           else 
           {
              *q = (int *)(*(isObjOrArray + 2));
           }
        }
        if(*isObjOrArray == 1)
        {
           if(*(isObjOrArray + 2) == 0)
           {
             int *s = (int *)(*q);
             *q = (int *)heap.toNext;
             gc_handle_array_obj(s);
           }
           else 
           {
              *q = (int *)(*(isObjOrArray + 2));
           }
        }
     }
     gc_top = (int *)(*gc_top);
  }
  //第一轮拷贝之后，依靠forwarding修正to堆中的对象的关系
  //由于在to堆中是依次存放的，所以依次遍历每个对象进行修复 
/*  int *p = (int *)heap.toStart;
  while(((int *)p) < ((int *)heap.toNext))
  {
     int *q = p + 1;
     //如果是普通对象
    //只需修复size部分，因为table部分指向的不是堆，前面已经拷贝过了，所以不用修复 
     if(*q == 0)
     {
        int *vtable = (int *)(*p);
        char *gc_map = (char *)(*vtable);
        int size = strlen(gc_map);
        //修复size部分
        int i;
        for(i = 0; i < size; i++)
        {
           if(*(gc_map + i) == '1')
           {
              //value就是类变量
              int *value = (int *)(*(p + 4 + i));
              if(((value) >= ((int *)heap.from)) && ((value) < ((int *)(heap.from + heap.size))))
              {
                 *(p + 4 + i) = *(value + 3);
                   printf("+++++p + 4 + i++++%x\n", p + 4 + i);
              }            
           }
           if(*(gc_map + i) == '0')
           {
              int *value = (int *)(*(p + 4 + i));
              printf("+++++value++++%d\n", value);
           }
        }
        p = p + 4 + size;       
     }
     //如果是对象数组
     //不需要任何的修复，只需p指针的移动
     if(*q == 1)
     {
        int length = (int)(*(p + 2));
        p = p + 4 + length;
     }
  }*/
  //拷贝之后，清空from堆，并交换from和to
  /*printf("before--heap.from--%x----\n",heap.from);
  printf("before--heap.toStart--%x----\n",heap.toStart);
  printf("before--heap.toNext--%x----\n",heap.toNext);
  printf("before--heap.fromFree--%x----\n",heap.fromFree);*/
  memset(heap.from,0,heap.size);
  char *temp;
  temp = heap.toStart;
  heap.toStart = heap.from;
  heap.from = temp;

  heap.fromFree = heap.toNext;
  heap.toNext = heap.toStart;
  
  /*printf("after--heap.from--%x----\n",heap.from);
  printf("after--heap.toStart--%x----\n",heap.toStart);
  printf("after--heap.toNext--%x----\n",heap.toNext);
  printf("after--heap.fromFree--%x----\n",heap.fromFree);*/
  int sum = heap.fromFree - heap.from;
//  sleep(1);
  gettimeofday(&t_end, NULL);
  int timeuse = 1000000 * (t_end.tv_sec - t_start.tv_sec) + t_end.tv_usec - t_start.tv_usec;
  if(Control_isPrintLog == 1)
  {
    FILE *fw;
    fw = fopen("../gc_log","a");
    if(!fw)
    {
      printf("file can not be opened!");
      exit(1);
    }
    //  printf("time is %d us\n",timeuse);
    fprintf(fw,"%d round of GC: %d us, collected %d bytes\n", j, timeuse, sum);
    fclose(fw);
  }
}

/*
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
*/
// "new" a new object, do necessary initializations, and
// return the pointer (reference).
/*    -----------------------------------------
      | vptr | v0 | v1 | ...      | v_{size-1}|                           
      -----------------------------------------
      ^      \                                /
      |       \<------------- size --------->/
      |
      p (returned address)
*/

/* void *Tiger_new (void *vtable, int size)
{
  // You should write 4 statements for this function.
  // #1: "malloc" a chunk of memory (be careful of the size) :
    int *p = (int*)malloc((size)*sizeof(char));

  // #2: clear this chunk of memory (zero off it):
    memset(p,0,sizeof(p));
   
  // #3: set up the "vptr" pointer to the value of "vtable":
     *p = (int)vtable;
    /* p++;
     for(int i = 0; i < sizeof(vtable)/4; i++) {
         *(p + i) = (int)((int *)vtable + i);
     } */
  
  // #4: return the pointer 
  //   return p;
//}*/


// "new" an array of size "length", do necessary
// initializations. And each array comes with an
// extra "header" storing the array length.
// This function should return the starting address
// of the array elements, but not the starting address of
// the array chunk. 
/*    ---------------------------------------------
      | length | e0 | e1 | ...      | e_{length-1}|                           
      ---------------------------------------------
               ^
               |
               p (returned address)
*/
/*void *Tiger_new_array (int length)
{
  // You can use the C "malloc" facilities, as above.
  // Your code here:
  int *p = (int*)malloc((length+1)*sizeof(int));
  *p = length;
  return p + 1;
}
>>>>>>> Lab3 */
