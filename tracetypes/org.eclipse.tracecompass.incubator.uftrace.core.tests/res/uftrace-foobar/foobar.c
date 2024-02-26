#include <pthread.h>

void *bar(void) {
  return NULL;
}

void *foo(void *unused) {
  return bar();
}

int main(int argc, char *argv[]) {
  pthread_t th;

  foo(argv);
  pthread_create(&th, NULL, foo, NULL);
  pthread_join(th, NULL);
  return 0;
}
